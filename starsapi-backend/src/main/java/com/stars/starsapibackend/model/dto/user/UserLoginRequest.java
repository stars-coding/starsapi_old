package com.stars.starsapibackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author stars
 */
@Data
public class UserLoginRequest implements Serializable {

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码（加密）
     */
    private String userPassword;

    private static final long serialVersionUID = 3191241716373120793L;
}
