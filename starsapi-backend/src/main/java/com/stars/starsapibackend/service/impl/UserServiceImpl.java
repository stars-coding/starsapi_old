package com.stars.starsapibackend.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stars.starsapibackend.common.ErrorCode;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.mapper.UserMapper;
import com.stars.starsapibackend.service.UserService;
import com.stars.starsapicommon.model.entity.User;
import com.stars.starsapicommon.model.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.stars.starsapibackend.constant.UserConstant.ADMIN_ROLE;
import static com.stars.starsapibackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现
 * 提供用户注册、登录、注销、获取用户信息、更新密钥、检查是否为管理员等功能。
 *
 * @author stars
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    private static final String SALT = "stars";

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 注册后的用户ID
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            QueryWrapper<User> queryWrapper = new QueryWrapper();
            queryWrapper.eq("userAccount", userAccount);
            long count = userMapper.selectCount(queryWrapper);
            if (count > 0L)
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            String accessKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(5));
            String secretKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(8));
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserName(userAccount);
            user.setUserPassword(encryptPassword);
            user.setAccessKey(accessKey);
            user.setSecretKey(secretKey);
            boolean saveResult = save(user);
            if (!saveResult)
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            return user.getId();
        }
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP请求对象
     * @return 登录成功的用户对象
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return user;
    }

    /**
     * 用户注销
     *
     * @param request HTTP请求对象
     * @return 注销是否成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null)
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取登录用户信息
     *
     * @param request HTTP请求对象
     * @return 登录用户对象
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        long userId = currentUser.getId();
        currentUser = (User) getById(userId);
        if (currentUser == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        return currentUser;
    }

    /**
     * 获取用户的视图对象
     *
     * @param user 用户对象
     * @return 用户的视图对象
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 更新用户的密钥
     *
     * @param id 用户ID
     * @return 是否成功更新密钥
     */
    @Override
    public boolean updateSecretKey(Long id) {
        User user = this.getById(id);
        String accessKey = DigestUtil.md5Hex(SALT + user.getUserAccount() + RandomUtil.randomNumbers(5));
        String secretKey = DigestUtil.md5Hex(SALT + user.getUserAccount() + RandomUtil.randomNumbers(8));
        user.setSecretKey(secretKey);
        user.setAccessKey(accessKey);
        return updateById(user);
    }

    /**
     * 检查用户是否为管理员
     *
     * @param request HTTP请求对象
     * @return 是否是管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && ADMIN_ROLE.equals(user.getUserRole());
    }
}
