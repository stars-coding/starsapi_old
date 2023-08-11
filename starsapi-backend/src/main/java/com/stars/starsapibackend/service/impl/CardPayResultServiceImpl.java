package com.stars.starsapibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stars.starsapibackend.mapper.CardPayResultMapper;
import com.stars.starsapibackend.service.CardPayResultService;
import com.stars.starsapicommon.model.entity.CardPayResult;
import org.springframework.stereotype.Service;

/**
 * 卡号支付结果服务接口
 * 提供卡号支付结果相关操作的接口定义。
 *
 * @author stars
 */
@Service
public class CardPayResultServiceImpl extends ServiceImpl<CardPayResultMapper, CardPayResult>
        implements CardPayResultService {

}
