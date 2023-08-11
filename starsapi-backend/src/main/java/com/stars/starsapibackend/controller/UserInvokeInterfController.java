package com.stars.starsapibackend.controller;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stars.starsapibackend.annotation.AuthCheck;
import com.stars.starsapibackend.common.*;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.model.dto.userinvokeinterf.UserInvokeInterfAddRequest;
import com.stars.starsapibackend.model.dto.userinvokeinterf.UserInvokeInterfQueryRequest;
import com.stars.starsapibackend.model.dto.userinvokeinterf.UserInvokeInterfUpdateRequest;
import com.stars.starsapibackend.service.*;
import com.stars.starsapicommon.model.entity.*;
import com.stars.starsapicommon.model.vo.SelfInterfDataVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.stars.starsapibackend.constant.RedisConstants.CACHE_MY_ORDERS_KEY;
import static com.stars.starsapibackend.constant.RedisConstants.LOCK_PAY_ORDER_KEY;
import static com.stars.starsapibackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户调用接口控制器
 *
 * @author stars
 */
@RestController
@Slf4j
@RequestMapping("/userInvokeInterf")
public class UserInvokeInterfController {

    @Resource
    private UserInvokeInterfService userInvokeInterfService;

    @Resource
    private InterfService interfService;

    @Resource
    private UserService userService;

    @Resource
    private OrdersService ordersService;

    @Resource
    private CardService cardService;

    @Resource
    private CardPayResultService cardPayResultService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户支付接口调用费用并增加调用次数
     *
     * @param interfName   调用的接口名称
     * @param cardNumber   充值卡号
     * @param cardPassword 充值卡密码
     * @param payAccount   充值账户
     * @param num          增加的调用次数
     * @param request      HTTP请求对象
     * @return 包含支付结果的响应对象
     */
    @PostMapping("/payInterf")
    // 出现检查异常也应该回滚
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<Boolean> payInterf(
            @RequestParam("interfName") String interfName,
            @RequestParam("cardNumber") String cardNumber,
            @RequestParam("cardPassword") String cardPassword,
            @RequestParam("payAccount") String payAccount,
            @RequestParam("num") long num, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        String key = LOCK_PAY_ORDER_KEY + loginUser.getId();
        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, key);
        // 加分布式锁
        boolean isLock = lock.tryLock(2400L);
        if (!isLock) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "请稍后再试");
        }
        // 根据传入的接口名称获取接口信息
        LambdaQueryWrapper<Interf> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Interf::getInterfName, interfName);
        Interf interf = interfService.getOne(lqw);
        Long interfId = interf.getId();
        // 根据充值账户查询用户是否存在
        LambdaQueryWrapper<User> lqw1 = new LambdaQueryWrapper<>();
        lqw1.eq(User::getUserAccount, payAccount);
        User user = userService.getOne(lqw1);
        if (user == null) {
            lock.unlock();
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        Long userId = user.getId();
        cardNumber = SecureUtil.md5(cardNumber);
        cardPassword = SecureUtil.md5(cardPassword);
        // 查询卡号和密码是否正确
        QueryWrapper<Card> cardQueryWrapper = new QueryWrapper<>();
        cardQueryWrapper.eq("cardNumber", cardNumber);
        cardQueryWrapper.eq("cardPassword", cardPassword);
        Card card = cardService.getOne(cardQueryWrapper);
        if (card == null) {
            lock.unlock();
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "卡号未找到");
        }
        // 若卡号正确，删除卡号
        boolean remove = cardService.remove(cardQueryWrapper);
        if (!remove) {
            ResultUtils.error(ErrorCode.SYSTEM_ERROR, "充值卡使用失败");
            lock.unlock();
        }
        // 充值完卡号，增加用户剩余调用次数
        LambdaQueryWrapper<UserInvokeInterf> lqw2 = new LambdaQueryWrapper<>();
        lqw2.eq(UserInvokeInterf::getUserId, userId);
        lqw2.eq(UserInvokeInterf::getInterfId, interfId);
        UserInvokeInterf one = userInvokeInterfService.getOne(lqw2);
        // 若用户已经拥有此接口，增加调用次数
        if (one != null) {
            one.setLeftInvokeNum(one.getLeftInvokeNum() + num);
            userInvokeInterfService.saveOrUpdate(one);
        } else {
            // 若没有，赋予用户拥有接口权限并增加次数
            UserInvokeInterf userInvokeInterf = new UserInvokeInterf();
            userInvokeInterf.setUserId(userId);
            userInvokeInterf.setInterfId(interfId);
            userInvokeInterf.setLeftInvokeNum(num);
            userInvokeInterfService.save(userInvokeInterf);
        }
        // 查询订单是否存在
        QueryWrapper<Orders> ordersQueryWrapper = new QueryWrapper<>();
        ordersQueryWrapper.eq("userId", userId);
        ordersQueryWrapper.eq("interfId", interfId);
        ordersQueryWrapper.eq("status", 0);
        Orders orders = ordersService.getOne(ordersQueryWrapper);
        if (orders == null) {
            lock.unlock();
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        // 如果存在订单，插入支付结算表
        CardPayResult payResult = new CardPayResult();
        payResult.setCardId(card.getId());
        payResult.setUserId(userId);
        payResult.setInterfId(interfId);
        payResult.setOrdersId(orders.getId());
        boolean save = cardPayResultService.save(payResult);
        if (!save) {
            lock.unlock();
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "支付失败");
        }
        String createTime = orders.getCreateTime().toString();
        DateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        String formattedDate = null;
        try {
            Date date = inputFormat.parse(createTime);
            formattedDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // 修改订单状态
        UpdateWrapper<Orders> ordersUpdateWrapper = new UpdateWrapper<>();
        ordersUpdateWrapper.eq("userId", userId);
        ordersUpdateWrapper.eq("interfId", interfId);
        ordersUpdateWrapper.eq("createTime", formattedDate);
        ordersUpdateWrapper.setSql("status = 2");
        boolean update = ordersService.update(ordersUpdateWrapper);
        if (update) {
            // todo 删除订单缓存
            String redisKey = CACHE_MY_ORDERS_KEY + userId;
            stringRedisTemplate.delete(redisKey);
            lock.unlock();
            return ResultUtils.success(update);
        }
        lock.unlock();
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统故障");
    }

    /**
     * 添加用户拥有的接口信息
     *
     * @param userInvokeInterfAddRequest 请求参数，包含用户接口信息
     * @param request                    HTTP请求对象
     * @return 包含新用户接口信息ID的响应对象
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> addUserInvokeInterf(@RequestBody UserInvokeInterfAddRequest userInvokeInterfAddRequest, HttpServletRequest request) {
        if (userInvokeInterfAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建并初始化用户接口对象
        UserInvokeInterf userInvokeInterf = new UserInvokeInterf();
        BeanUtils.copyProperties(userInvokeInterfAddRequest, userInvokeInterf);
        userInvokeInterfService.validUserInvokeInterf(userInvokeInterf, true);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        userInvokeInterf.setUserId(loginUser.getId());
        // 保存用户接口信息
        boolean result = userInvokeInterfService.save(userInvokeInterf);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newUserInvokeInterfId = userInvokeInterf.getId();
        return ResultUtils.success(newUserInvokeInterfId);
    }

    /**
     * 删除用户拥有的接口信息
     *
     * @param deleteRequest 请求参数，包含待删除的用户接口信息ID
     * @param request       HTTP请求对象
     * @return 包含删除操作结果的响应对象
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteUserInvokeInterf(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户和待删除的用户接口信息
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        UserInvokeInterf oldUserInvokeInterf = userInvokeInterfService.getById(id);
        if (oldUserInvokeInterf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查权限
        if (!oldUserInvokeInterf.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 执行删除操作
        boolean b = userInvokeInterfService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新用户拥有的接口信息
     *
     * @param userInvokeInterfUpdateRequest 请求参数，包含用户接口信息更新内容
     * @param request                       HTTP请求对象
     * @return 包含更新操作结果的响应对象
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> updateUserInvokeInterf(@RequestBody UserInvokeInterfUpdateRequest userInvokeInterfUpdateRequest, HttpServletRequest request) {
        if (userInvokeInterfUpdateRequest == null || userInvokeInterfUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建并初始化用户接口对象
        UserInvokeInterf userInvokeInterf = new UserInvokeInterf();
        BeanUtils.copyProperties(userInvokeInterfUpdateRequest, userInvokeInterf);
        userInvokeInterfService.validUserInvokeInterf(userInvokeInterf, false);
        // 获取当前登录用户和待更新的用户接口信息
        User user = userService.getLoginUser(request);
        long id = userInvokeInterfUpdateRequest.getId();
        UserInvokeInterf oldUserInvokeInterf = userInvokeInterfService.getById(id);
        if (oldUserInvokeInterf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查权限
        if (!oldUserInvokeInterf.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 执行更新操作
        boolean result = userInvokeInterfService.updateById(userInvokeInterf);
        return ResultUtils.success(result);
    }

    /**
     * 根据ID获取用户拥有的接口信息
     *
     * @param id 用户接口信息ID
     * @return 包含用户接口信息的响应对象
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<UserInvokeInterf> getUserInvokeInterfById(long id) {
        if (id <= 0L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInvokeInterf userInvokeInterf = userInvokeInterfService.getById(id);
        return ResultUtils.success(userInvokeInterf);
    }

    /**
     * 获取用户拥有的接口信息列表
     *
     * @param userInvokeInterfQueryRequest 查询条件
     * @return 包含用户接口信息列表的响应对象
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<UserInvokeInterf>> listUserInvokeInterf(UserInvokeInterfQueryRequest userInvokeInterfQueryRequest) {
        UserInvokeInterf userInvokeInterfQuery = new UserInvokeInterf();
        if (userInvokeInterfQueryRequest != null) {
            BeanUtils.copyProperties(userInvokeInterfQueryRequest, userInvokeInterfQuery);
        }
        QueryWrapper<UserInvokeInterf> queryWrapper = new QueryWrapper<>(userInvokeInterfQuery);
        List<UserInvokeInterf> userInvokeInterfList = userInvokeInterfService.list(queryWrapper);
        return ResultUtils.success(userInvokeInterfList);
    }

    /**
     * 分页获取用户拥有的接口信息列表
     *
     * @param userInvokeInterfQueryRequest 查询条件
     * @param request                      HTTP请求对象
     * @return 包含用户接口信息分页数据的响应对象
     */
    @GetMapping("/list/page")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Page<UserInvokeInterf>> listUserInvokeInterfByPage(UserInvokeInterfQueryRequest userInvokeInterfQueryRequest, HttpServletRequest request) {
        if (userInvokeInterfQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInvokeInterf userInvokeInterfQuery = new UserInvokeInterf();
        BeanUtils.copyProperties(userInvokeInterfQueryRequest, userInvokeInterfQuery);
        long current = userInvokeInterfQueryRequest.getCurrent();
        long size = userInvokeInterfQueryRequest.getPageSize();
        String sortField = userInvokeInterfQueryRequest.getSortField();
        String sortOrder = userInvokeInterfQueryRequest.getSortOrder();
        if (size > 50L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<UserInvokeInterf> queryWrapper = new QueryWrapper<>(userInvokeInterfQuery);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        Page<UserInvokeInterf> userInvokeInterfPage = userInvokeInterfService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(userInvokeInterfPage);
    }

    /**
     * 获取用户拥有的接口剩余调用次数
     *
     * @param request HTTP请求对象
     * @return 包含用户接口剩余调用次数的响应对象
     */
    @GetMapping("/selfInterfData")
    public BaseResponse<List<SelfInterfDataVO>> selfInterfData(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        Long id = currentUser.getId();
        LambdaQueryWrapper<UserInvokeInterf> lqw = new LambdaQueryWrapper<>();
        lqw.eq(UserInvokeInterf::getUserId, id);
        List<UserInvokeInterf> list = userInvokeInterfService.list(lqw);
        List<SelfInterfDataVO> selfInterfDataVOs = new ArrayList<>();
        for (UserInvokeInterf userInvokeInterf : list) {
            SelfInterfDataVO selfInterfDataVO = new SelfInterfDataVO();
            BeanUtils.copyProperties(userInvokeInterf, selfInterfDataVO);
            Long interfId = userInvokeInterf.getInterfId();
            LambdaQueryWrapper<Interf> lqw1 = new LambdaQueryWrapper<>();
            lqw1.eq(Interf::getId, interfId);
            Interf one = interfService.getOne(lqw1);
            String name = one.getInterfName();
            selfInterfDataVO.setInterfName(name);
            selfInterfDataVOs.add(selfInterfDataVO);
        }
        return ResultUtils.success(selfInterfDataVOs);
    }
}
