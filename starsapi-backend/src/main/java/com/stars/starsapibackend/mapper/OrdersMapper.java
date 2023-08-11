package com.stars.starsapibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stars.starsapicommon.model.entity.Orders;
import com.stars.starsapicommon.model.vo.OrdersVO;

import java.util.List;

/**
 * @Entity com.stars.starsapicommon.model.entity.Orders
 */
public interface OrdersMapper extends BaseMapper<Orders> {

    List<OrdersVO> selectMyOrders(long userId, int start, long pageSize);
}
