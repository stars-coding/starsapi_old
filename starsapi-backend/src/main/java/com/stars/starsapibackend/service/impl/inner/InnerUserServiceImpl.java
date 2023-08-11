package com.stars.starsapibackend.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stars.starsapibackend.common.ErrorCode;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.mapper.UserMapper;
import com.stars.starsapicommon.model.entity.User;
import com.stars.starsapicommon.service.InnerUserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 内部用户服务实现
 * 该服务提供了获取已分配密钥的用户的功能。
 *
 * @author stars
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取调用用户
     * 从数据库中查询已分配密钥的用户。
     *
     * @param accessKey 密钥
     * @return 如果查询成功，返回User对象；否则返回null
     */
    @Override
    public User getInvokeUser(String accessKey) {
        if (StringUtils.isAnyBlank(accessKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getAccessKey, accessKey);
        return userMapper.selectOne(lqw);
    }
}
