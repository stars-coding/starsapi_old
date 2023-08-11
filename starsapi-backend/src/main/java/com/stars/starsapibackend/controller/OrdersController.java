package com.stars.starsapibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.stars.starsapibackend.common.*;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.model.dto.orders.OrdersDeleteRequest;
import com.stars.starsapibackend.model.dto.orders.OrdersQueryRequest;
import com.stars.starsapibackend.mq.QueueMessageService;
import com.stars.starsapibackend.service.InterfService;
import com.stars.starsapibackend.service.OrdersService;
import com.stars.starsapibackend.service.UserService;
import com.stars.starsapicommon.model.entity.Interf;
import com.stars.starsapicommon.model.entity.Orders;
import com.stars.starsapicommon.model.entity.User;
import com.stars.starsapicommon.model.vo.OrdersVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.stars.starsapibackend.constant.RedisConstants.CACHE_MY_ORDERS_KEY;
import static com.stars.starsapibackend.constant.RedisConstants.LOCK_ADD_ORDER_KEY;

/**
 * 订单控制器
 *
 * @author stars
 */
@RestController
@RequestMapping("/orders")
@Slf4j
public class OrdersController {

    @PostConstruct
    private void init() {
        ORDER_EXECUTOR.submit(new OrdersHandler());
    }

    // 创建阻塞队列
    private BlockingQueue<Orders> ordersTasks = new ArrayBlockingQueue<>(1024 * 1024);

    // 创建线程池
    private static final ExecutorService ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @Resource
    private OrdersService ordersService;

    @Resource
    private InterfService interfService;

    @Resource
    private UserService userService;

    @Resource
    private QueueMessageService queueMessageService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 异步下单操作
     *
     * @param interfName         接口名称
     * @param payAccount         支付账户
     * @param num                充值次数
     * @param httpServletRequest HTTP请求对象
     * @return 包含操作结果的响应对象
     */
    @PostMapping("/add")
    public BaseResponse<Boolean> addOrders(@RequestParam String interfName,
                                           @RequestParam String payAccount,
                                           @RequestParam long num,
                                           HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 查询接口信息
        QueryWrapper<Interf> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("interfName", interfName);
        Interf interf = interfService.getOne(queryWrapper);
        if (interf == null) {
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        // 查询用户信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", payAccount);
        User user = userService.getOne(userQueryWrapper);
        if (user == null) {
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        if (num > 200L) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "参数过大");
        }
        // 创建订单
        Orders orders = new Orders();
        orders.setInterfId(interf.getId());
        orders.setUserId(user.getId());
        orders.setRechargeTimes(num);
        // 将订单添加到队列，异步处理
        ordersTasks.add(orders);
        Long userId = user.getId();
        // todo 添加订单缓存
        String redisKey = CACHE_MY_ORDERS_KEY + userId;
        Gson gson = new Gson();
        String value = gson.toJson(orders);
        stringRedisTemplate.opsForValue().set(redisKey, value);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取当前用户的订单列表
     *
     * @param ordersQueryRequest 包含查询条件的请求对象
     * @param request            HTTP请求对象
     * @return 分页订单列表的响应对象
     */
    @GetMapping("/list/page")
    public BaseResponse<PageHelper<OrdersVO>> listMyOrdersByPage(OrdersQueryRequest ordersQueryRequest, HttpServletRequest request) {
        // 检查请求参数是否为空
        if (ordersQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        // 获取当前登录用户的ID
        Long id = this.userService.getLoginUser(request).getId();
        ordersQueryRequest.setUserId(id);
        // 调用订单服务，获取分页订单列表
        PageHelper<OrdersVO> myOrdersList = ordersService.getMyOrders(ordersQueryRequest);
        return ResultUtils.success(myOrdersList);
    }

    /**
     * 删除订单和支付结果
     *
     * @param ordersDeleteRequest 包含订单ID和状态的请求对象
     * @param httpServletRequest  HTTP请求对象
     * @return 删除操作结果的响应对象
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteOrdersAndPayResult(@RequestBody OrdersDeleteRequest ordersDeleteRequest, HttpServletRequest httpServletRequest) {
        String dateString = ordersDeleteRequest.getCreateTime().toString();
        // 格式化日期字符串
        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = inputFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "日期格式解析错误");
        }
        String createTime = outputFormat.format(date);
        // 检查请求参数是否为空或无效
        if (ordersDeleteRequest == null || ordersDeleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数无效");
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId = loginUser.getId();
        Long ordersId = ordersDeleteRequest.getId();
        int status = ordersDeleteRequest.getStatus();
        // 调用订单服务，删除订单和支付结果
        Boolean result = ordersService.deleteOrdersAndPayResult(ordersId, userId, status, createTime);
        return ResultUtils.success(result);
    }

    /**
     * 内部类，用于处理订单的异步任务
     *
     * @author stars
     */
    private class OrdersHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                Orders orders = null;
                try {
                    // 从订单任务队列中获取订单
                    orders = ordersTasks.take();
                    Long userId = orders.getUserId();
                    Long interfId = orders.getInterfId();
                    long num = orders.getRechargeTimes();
                    // 处理订单
                    handleOrders(userId, interfId, num);
                } catch (InterruptedException e) {
                    log.error("订单创建异常");
                    e.printStackTrace();
                }
            }
        }

        /**
         * 处理订单的方法
         *
         * @param userId   用户ID
         * @param interfId 接口ID
         * @param num      充值次数
         */
        private void handleOrders(long userId, long interfId, long num) {
            String key = LOCK_ADD_ORDER_KEY + userId;
            SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, key);
            boolean isLock = lock.tryLock(1200L);
            if (!isLock) {
                log.error("处理订单失败");
                return;
            }
            // 调用订单服务，创建订单
            ordersService.createOrders(userId, interfId, num);
            lock.unlock();
        }
    }
}
