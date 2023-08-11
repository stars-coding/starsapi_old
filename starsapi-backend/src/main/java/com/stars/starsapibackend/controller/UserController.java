package com.stars.starsapibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stars.starsapibackend.annotation.AuthCheck;
import com.stars.starsapibackend.common.*;
import com.stars.starsapibackend.constant.RedisConstants;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.model.dto.user.*;
import com.stars.starsapibackend.service.CardService;
import com.stars.starsapibackend.service.UserService;
import com.stars.starsapicommon.model.entity.User;
import com.stars.starsapicommon.model.vo.UserVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.stars.starsapibackend.constant.RedisConstants.CACHE_USERINFO_KEY;

/**
 * 用户控制器
 *
 * @author stars
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private CardService cardService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册请求对象
     * @return 注册后的用户ID
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 检查请求是否为空
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 检查参数是否为空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 调用用户服务的注册方法
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求对象
     * @param request          HTTP请求对象
     * @return 登录成功的用户对象
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 检查请求是否为空
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 检查参数是否为空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 调用用户服务的登录方法
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request HTTP请求对象
     * @return 注销是否成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 检查请求是否为空
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 调用用户服务的注销方法
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest 更新用户请求对象
     * @param request           HTTP请求对象
     * @return 是否成功更新用户信息
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        // 检查请求是否为空，以及ID是否为空
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 创建用户对象并从请求对象中复制属性
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 调用用户服务的更新用户信息方法
        boolean result = userService.updateById(user);
        // 删除缓存中的用户信息
        String redisKey = CACHE_USERINFO_KEY + user.getId();
        this.stringRedisTemplate.delete(redisKey);
        return ResultUtils.success(result);
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param id      用户ID
     * @param request HTTP请求对象
     * @return 用户对象
     */
    @GetMapping("/get")
    public BaseResponse<User> getUserById(long id, HttpServletRequest request) {
        // 检查ID是否合法
        if (id <= 0L) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 调用用户服务的根据ID获取用户信息方法
        User user = userService.getById(id);
        return ResultUtils.success(user);
    }

    /**
     * 列出所有用户
     *
     * @param userQueryRequest 用户查询请求对象
     * @param request          HTTP请求对象
     * @return 用户列表
     */
    @GetMapping("/list")
    public BaseResponse<List<UserVO>> listUser(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        // 创建用户查询对象并从请求对象中复制属性
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
        }
        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        // 调用用户服务的查询方法
        List<User> userList = userService.list(queryWrapper);
        // 将用户列表转换为用户视图对象列表
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页列出用户
     *
     * @param userQueryRequest 用户查询请求对象
     * @param request          HTTP请求对象
     * @return 分页后的用户列表
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<UserVO>> listUserByPage(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        long current = 1L;
        long size = 10L;
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
            current = userQueryRequest.getCurrent();
            size = userQueryRequest.getPageSize();
        }
        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        // 调用用户服务的分页查询方法
        Page<User> userPage = userService.page(new Page<>(current, size), queryWrapper);
        // 构建分页对象并设置查询结果
        Page<UserVO> userVOPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 根据用户ID获取用户视图对象
     *
     * @param id      用户ID
     * @param request HTTP请求对象
     * @return 用户视图对象
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id, HttpServletRequest request) {
        String redisKey = CACHE_USERINFO_KEY + id;
        String userInfo = stringRedisTemplate.opsForValue().get(redisKey);
        if (userInfo != null && userInfo.length() != 0) {
            Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new FormatUtils()).create();
            UserVO userVO1 = (UserVO) gson.fromJson(userInfo, UserVO.class);
            return ResultUtils.success(userVO1);
        }
        BaseResponse<User> response = getUserById(id, request);
        User user = (User) response.getData();
        UserVO userVO = userService.getUserVO(user);
        try {
            String userInfoJSON = new ObjectMapper().writeValueAsString(userVO);
            this.stringRedisTemplate.opsForValue().set(redisKey, userInfoJSON);
            this.stringRedisTemplate.expire(redisKey, RedisConstants.USER_INFO_TIME_OUT, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return ResultUtils.success(userVO);
    }

    /**
     * 更新用户的密钥
     *
     * @param idRequest 更新密钥请求对象
     * @param request   HTTP请求对象
     * @return 是否成功更新密钥
     */
    @PostMapping("/update/secret_key")
    public BaseResponse<Boolean> updateSecretKey(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        // 检查请求是否为空，以及ID是否为空
        if (idRequest == null || idRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 调用用户服务的更新密钥方法
        boolean result = this.userService.updateSecretKey(idRequest.getId());
        // 删除缓存中的用户信息
        String redisKey = CACHE_USERINFO_KEY + idRequest.getId();
        this.stringRedisTemplate.delete(redisKey);
        return ResultUtils.success(true);
    }

    /**
     * 添加卡号
     *
     * @return 是否成功添加卡号
     * @throws IOException
     */
    @PostMapping("/card/add")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> addCard() throws IOException {
        // 添加卡号的控制器方法，需要管理员权限
        boolean result = this.cardService.generateCard();
        if (result) {
            return ResultUtils.success(result);
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "");
    }
}
