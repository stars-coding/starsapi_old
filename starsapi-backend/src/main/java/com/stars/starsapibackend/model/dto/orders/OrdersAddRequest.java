package com.stars.starsapibackend.model.dto.orders;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单添加请求
 *
 * @author stars
 */
@Data
public class OrdersAddRequest implements Serializable {

    /**
     * 用户主键
     */
    private Long userId;

    /**
     * 接口主键
     */
    private Long interfId;

    /**
     * 充值次数
     */
    private Long rechargeTimes;

    private static final long serialVersionUID = 1L;
}
