package com.stars.starsapibackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图
 *
 * @author stars
 */
@Data
public class UserVO implements Serializable {

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
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
