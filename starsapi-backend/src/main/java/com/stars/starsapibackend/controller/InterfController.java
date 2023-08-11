package com.stars.starsapibackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stars.starsapibackend.annotation.AuthCheck;
import com.stars.starsapibackend.common.*;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.model.dto.interf.InterfAddRequest;
import com.stars.starsapibackend.model.dto.interf.InterfInvokeRequest;
import com.stars.starsapibackend.model.dto.interf.InterfQueryRequest;
import com.stars.starsapibackend.model.dto.interf.InterfUpdateRequest;
import com.stars.starsapibackend.model.enums.InterfStatusEnum;
import com.stars.starsapibackend.service.InterfService;
import com.stars.starsapibackend.service.UserInvokeInterfService;
import com.stars.starsapibackend.service.UserService;
import com.stars.starsapiclientsdk.client.StarsApiClient;
import com.stars.starsapicommon.model.entity.*;
import com.stars.starsapicommon.model.vo.InterfVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.stars.starsapibackend.constant.CommonConstant.SORT_ORDER_ASC;
import static com.stars.starsapibackend.constant.RedisConstants.CACHE_INTERF_KEY;
import static com.stars.starsapibackend.constant.RedisConstants.LOCK_INTERF_BY_BREAK;

/**
 * 接口控制器
 *
 * @author stars
 */
@RestController
@Slf4j
@RequestMapping("/interf")
public class InterfController {

    @Resource
    private InterfService interfService;

    @Resource
    private UserInvokeInterfService userInvokeInterfService;

    @Resource
    private UserService userService;

    @Resource
    private StarsApiClient starsApiClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加接口
     *
     * @param interfAddRequest 添加接口请求对象
     * @param request          HTTP请求对象
     * @return 新增接口的ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addInterf(@RequestBody InterfAddRequest interfAddRequest, HttpServletRequest request) {
        // 检查请求是否为空
        if (interfAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建接口对象并从请求对象中复制属性
        Interf interf = new Interf();
        BeanUtils.copyProperties(interfAddRequest, interf);
        // 验证接口信息
        interfService.validInterf(interf, true);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        interf.setInterfUserId(userId);
        // 调用接口服务的保存接口方法
        boolean result = interfService.save(interf);
        // 检查是否保存成功
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        // 增加用户接口调用计数
        this.userInvokeInterfService.addInvokeCount(interf.getId(), userId);
        // 获取新创建接口的ID并返回
        long newInterfId = interf.getId();
        return ResultUtils.success(newInterfId);
    }

    /**
     * 删除接口
     *
     * @param deleteRequest 删除接口请求对象
     * @param request       HTTP请求对象
     * @return 是否成功删除接口
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterf(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 检查请求是否为空，以及接口ID是否合法
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 查询待删除的接口
        Interf oldInterf = interfService.getById(id);
        // 检查接口是否存在
        if (oldInterf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查权限，只有接口创建者或管理员可以删除接口
        if (!oldInterf.getInterfUserId().equals(user.getId()) && !this.userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 调用接口服务的删除接口方法
        boolean b = interfService.removeById(id);
        // 构建用户接口调用对象，用于删除接口调用计数
        UserInvokeInterf userInvokeInterf = new UserInvokeInterf();
        userInvokeInterf.setInterfId(id);
        // 删除接口，将缓存删除
        stringRedisTemplate.delete(CACHE_INTERF_KEY + id);
        // 检查是否删除成功
        if (b) {
            boolean result = userInvokeInterfService.removeById(userInvokeInterf);
            return ResultUtils.success(result);
        }
        return ResultUtils.success(b);
    }

    /**
     * 更新接口信息
     *
     * @param interfUpdateRequest 更新接口请求对象
     * @param request             HTTP请求对象
     * @return 是否成功更新接口信息
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterf(@RequestBody InterfUpdateRequest interfUpdateRequest,
                                              HttpServletRequest request) {
        // 检查请求是否为空，以及接口ID是否合法
        if (interfUpdateRequest == null || interfUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建接口对象并从请求对象中复制属性
        Interf interf = new Interf();
        BeanUtils.copyProperties(interfUpdateRequest, interf);
        // 验证接口信息
        this.interfService.validInterf(interf, false);
        // 获取当前登录用户
        User user = this.userService.getLoginUser(request);
        long id = interfUpdateRequest.getId();
        // 查询待更新的接口
        Interf oldInterf = interfService.getById(id);
        // 检查接口是否存在
        if (oldInterf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查权限，只有接口创建者或管理员可以更新接口信息
        if (!oldInterf.getInterfUserId().equals(user.getId()) && !this.userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 调用接口服务的更新接口信息方法
        boolean result = interfService.updateById(interf);
        // 删除缓存中的接口信息
        String key = CACHE_INTERF_KEY + id;
        this.stringRedisTemplate.delete(key);
        return ResultUtils.success(result);
    }

    /**
     * 获取指定ID的接口信息
     *
     * @param id 接口的唯一标识符
     * @return 包含接口信息的响应对象
     */
    @GetMapping("/get")
    public BaseResponse<Interf> getInterfById(long id) {
        // 检查参数是否合法
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 1、尝试从缓存中获取接口信息
        String key = CACHE_INTERF_KEY + id;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        String o = (String) entries.get("");
        // 如果缓存存在且不为空
        if (CollUtil.isNotEmpty(entries) && !"".equals(o)) {
            // 直接从缓存中封装数据并返回
            Interf interf = BeanUtil.fillBeanWithMap(entries, new Interf(), false);
            return ResultUtils.success(interf);
        }
        // 如果缓存为""，表示缓存穿透，返回错误信息
        if ("".equals(o)) {
            return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "接口信息未找到");
        }
        // 使用互斥锁解决缓存击穿
        SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, LOCK_INTERF_BY_BREAK + id);
        Interf interf = null;
        try {
            // 尝试获取锁，最多等待1200毫秒
            boolean isLock = lock.tryLock(1200);
            // 如果获取锁失败，等待50毫秒后再次尝试
            if (!isLock) {
                Thread.sleep(50);
                return getInterfById(id);
            }
            // 1.2、缓存不存在，去数据库查询数据
            interf = interfService.getById(id);
            // 2、数据库数据不存在，返回错误信息+缓存穿透
            if (BeanUtil.isEmpty(interf)) {
                // 缓存空对象，解决缓存穿透
                stringRedisTemplate.opsForHash().put(key, "", "");
                stringRedisTemplate.expire(key, 30L, TimeUnit.MINUTES);
                return ResultUtils.error(ErrorCode.NOT_FOUND_ERROR, "接口信息未找到");
            }
            // 3、数据库数据存在，封装map对象放到缓存中
            Map<String, Object> map = BeanUtil.beanToMap(interf, new HashMap<>(),
                    CopyOptions.create().setIgnoreNullValue(false)
                            .setFieldValueEditor((keys, values) -> {
                                if (values == null) {
                                    values = null;
                                } else {
                                    values = values.toString();
                                }
                                return values;
                            }));
            // 3.1、将map对象放到redis并设置超时时间
            stringRedisTemplate.opsForHash().putAll(key, map);
            stringRedisTemplate.expire(key, 30L, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 释放互斥锁
            lock.unlock();
        }
        // 返回查询到的接口信息
        return ResultUtils.success(interf);
    }

    /**
     * 列出所有接口（需要管理员权限）
     *
     * @param interfQueryRequest 包含接口查询条件的请求体
     * @return 包含接口列表的响应对象
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<Interf>> listInterf(InterfQueryRequest interfQueryRequest) {
        Interf interfQuery = new Interf();
        if (interfQueryRequest != null) {
            BeanUtils.copyProperties(interfQueryRequest, interfQuery);
        }
        QueryWrapper<Interf> queryWrapper = new QueryWrapper<>(interfQuery);
        List<Interf> interfList = interfService.list(queryWrapper);
        return ResultUtils.success(interfList);
    }

    /**
     * 分页列出接口
     *
     * @param interfQueryRequest 包含接口查询条件的请求体
     * @param request            HTTP请求对象
     * @return 包含分页接口列表的响应对象
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Interf>> listInterfByPage(InterfQueryRequest interfQueryRequest, HttpServletRequest request) {
        if (interfQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Interf interfQuery = new Interf();
        BeanUtils.copyProperties(interfQueryRequest, interfQuery);
        long current = interfQueryRequest.getCurrent();
        long size = interfQueryRequest.getPageSize();
        String sortField = interfQueryRequest.getSortField();
        String sortOrder = interfQueryRequest.getSortOrder();
        String interfDescription = interfQuery.getInterfDescription();
        interfQuery.setInterfDescription(null);
        if (size > 50L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<Interf> queryWrapper = new QueryWrapper<>(interfQuery);
        queryWrapper.like(StringUtils.isNotBlank(interfDescription), "interfDescription", interfDescription);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), sortOrder.equals(SORT_ORDER_ASC), sortField);
        Page<Interf> interfPage = interfService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(interfPage);
    }

    /**
     * 将接口发布（需要管理员权限）
     *
     * @param idRequest 包含待发布接口ID的请求体
     * @param request   HTTP请求对象
     * @return 包含发布结果的响应对象
     */
    @PostMapping("/online")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> onlineInterf(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        Interf interf = (Interf) this.interfService.getById(id);
        if (interf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Interf updateInterf = new Interf();
        updateInterf.setId(id);
        updateInterf.setInterfStatus(InterfStatusEnum.ONLINE.getValue());
        boolean result = interfService.updateById(updateInterf);
        String key = CACHE_INTERF_KEY + id;
        stringRedisTemplate.delete(key);
        return ResultUtils.success(result);
    }

    /**
     * 将接口下线（需要管理员权限）
     *
     * @param idRequest 包含待下线接口ID的请求体
     * @param request   HTTP请求对象
     * @return 包含下线结果的响应对象
     */
    @PostMapping("/offline")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> offlineInterf(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        Interf interf = interfService.getById(id);
        if (interf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Interf updateInterf = new Interf();
        updateInterf.setId(id);
        updateInterf.setInterfStatus(InterfStatusEnum.OFFLINE.getValue());
        boolean result = interfService.updateById(updateInterf);
        String key = CACHE_INTERF_KEY + id;
        stringRedisTemplate.delete(key);
        return ResultUtils.success(result);
    }

    /**
     * 调用接口
     *
     * @param interfInvokeRequest 包含接口调用请求参数的请求体
     * @param request             HTTP请求对象
     * @return 包含接口调用结果的响应对象
     */
    @PostMapping("/invoke")
    public BaseResponse<Object> invokeInterf(@RequestBody InterfInvokeRequest interfInvokeRequest, HttpServletRequest request) {
        if (interfInvokeRequest == null || interfInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = interfInvokeRequest.getId();
        String userRequestParams = interfInvokeRequest.getUserRequestParams();
        // 1、判断接口是否存在
        Interf interf = interfService.getById(id);
        if (interf == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (interf.getInterfStatus() == InterfStatusEnum.OFFLINE.getValue()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口已关闭");
        }
        // 2、判断用户是否有此接口的调用权限
        String url = interf.getInterfUrl();
        String method = interf.getInterfRequestMethod();
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserInvokeInterf> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.eq("interfId", interf.getId());
        long count = userInvokeInterfService.count(queryWrapper);
        if (count == 0) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "此接口暂无调用次数，请前往充值");
        }
        // 3、获取用户唯一key
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();
        StarsApiClient starsApiClient = new StarsApiClient(accessKey, secretKey);
        // 4、进行调用
        if (id == 1) {
            TestUser testUser = JSON.parseObject(userRequestParams, TestUser.class);
            String parameters = JSON.toJSONString(testUser);
            String result = starsApiClient.onlineInvoke(parameters, url, method);
            return ResultUtils.success(result);
        } else if (id == 28) {
            MLyParams mLyParams = JSON.parseObject(userRequestParams, MLyParams.class);
            String parameters = JSON.toJSONString(mLyParams);
            String result = starsApiClient.onlineInvoke(parameters, url, method);
            return ResultUtils.success(result);
        } else if (id == 32) {
            QYKParams qykParams = JSON.parseObject(userRequestParams, QYKParams.class);
            String parameters = JSON.toJSONString(qykParams);
            String result = starsApiClient.onlineInvoke(parameters, url, method);
            return ResultUtils.success(result);
        }
        String result = starsApiClient.onlineInvoke(userRequestParams, url, method);
        return ResultUtils.success(result);
    }

    /**
     * 获取所有接口的名称列表
     *
     * @return 包含接口名称列表的响应对象
     */
    @GetMapping("/interfNameList")
    public BaseResponse<Map> interfNameList() {
        // 从数据库获取所有接口信息
        List<Interf> list = this.interfService.list();
        // 创建一个映射，用于存储接口名称和接口名称的映射关系
        Map<Object, Object> interfNameMap = new HashMap<>();
        // 遍历接口列表，将接口名称添加到映射中
        for (Interf interf : list) {
            String name = interf.getInterfName();
            interfNameMap.put(interf.getInterfName(), interf.getInterfName());
        }
        // 返回包含接口名称映射的响应
        return ResultUtils.success(interfNameMap);
    }

    /**
     * 获取当前用户拥有的接口列表（需要用户登录）
     *
     * @param interfQueryRequest 包含查询条件的请求对象
     * @param request            HTTP请求对象
     * @return 包含当前用户接口列表的响应对象
     */
    @GetMapping("/myInterf")
    public BaseResponse<PageHelper<InterfVO>> selectMyInterf(InterfQueryRequest interfQueryRequest, HttpServletRequest request) {
        // 获取当前登录用户的ID
        Long id = userService.getLoginUser(request).getId();
        // 设置查询条件中的用户ID为当前用户ID
        interfQueryRequest.setInterfUserId(id);
        // 查询当前用户拥有的接口信息
        PageHelper<InterfVO> myInterf = interfService.getMyInterf(interfQueryRequest);
        // 返回包含当前用户接口信息的响应
        return ResultUtils.success(myInterf);
    }

    /**
     * 获取SDK开发工具包
     *
     * @param response HTTP响应对象
     * @return
     */
    @GetMapping("/sdk")
    public void getSdk(HttpServletResponse response) throws IOException {
        // 获取要下载的文件
        org.springframework.core.io.Resource resource = new ClassPathResource("starsapi-client-sdk-0.0.1.jar");
        InputStream inputStream = resource.getInputStream();
        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=starsapi-client-sdk-0.0.1.jar");
        // 将文件内容写入响应
        try (OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        } catch (IOException e) {
            // 处理异常
            e.printStackTrace();
        } finally {
            inputStream.close();
        }
    }
}
