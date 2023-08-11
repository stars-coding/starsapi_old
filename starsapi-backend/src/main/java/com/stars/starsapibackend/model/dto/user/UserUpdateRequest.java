package com.stars.starsapibackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求
 *
 * @author stars
 */
@Data
public class UserUpdateRequest implements Serializable {

    /**
     * 用户主键
     */
    private Long id;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户性别(0-女，1-男)
     */
    private Integer userGender;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码（加密）
     */
    private String userPassword;

    private static final long serialVersionUID = 1L;
}
