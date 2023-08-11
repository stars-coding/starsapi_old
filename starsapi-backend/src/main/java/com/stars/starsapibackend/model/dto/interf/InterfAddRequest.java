package com.stars.starsapibackend.model.dto.interf;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口添加请求
 *
 * @author stars
 */
@Data
public class InterfAddRequest implements Serializable {

    /**
     * 接口名称
     */
    private String interfName;

    /**
     * 接口描述
     */
    private String interfDescription;

    /**
     * 接口地址
     */
    private String interfUrl;

    /**
     * 接口请求方法
     */
    private String interfRequestMethod;

    /**
     * 接口请求参数
     */
    private String interfRequestParams;

    /**
     * 接口请求头
     */
    private String interfRequestHeader;

    /**
     * 接口响应头
     */
    private String interfResponseHeader;

    private static final long serialVersionUID = 1L;
}
