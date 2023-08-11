package com.stars.starsapibackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stars.starsapicommon.model.entity.User;
import com.stars.starsapicommon.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户服务接口
 * 提供用户相关操作的接口定义，包括用户注册、用户登录、用户注销、获取登录用户信息等。
 *
 * @author stars
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 注册后的用户ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP请求对象
     * @return 登录成功的用户对象
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request HTTP请求对象
     * @return 注销是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取登录用户信息
     *
     * @param request HTTP请求对象
     * @return 登录用户对象
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取用户的视图对象
     *
     * @param user 用户对象
     * @return 用户的视图对象
     */
    UserVO getUserVO(User user);

    /**
     * 更新用户的密钥
     *
     * @param id 用户ID
     * @return 是否成功更新密钥
     */
    boolean updateSecretKey(Long id);

    /**
     * 检查用户是否为管理员
     *
     * @param request HTTP请求对象
     * @return 是否是管理员
     */
    boolean isAdmin(HttpServletRequest request);
}
