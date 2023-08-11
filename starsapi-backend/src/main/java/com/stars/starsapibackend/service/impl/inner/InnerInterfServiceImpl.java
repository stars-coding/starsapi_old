package com.stars.starsapibackend.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stars.starsapibackend.common.ErrorCode;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.mapper.InterfMapper;
import com.stars.starsapicommon.model.entity.Interf;
import com.stars.starsapicommon.service.InnerInterfService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 内部接口服务实现
 * 该服务提供了查询接口是否存在于数据库中的功能。
 *
 * @author stars
 */
@DubboService
public class InnerInterfServiceImpl implements InnerInterfService {

    @Autowired
    private InterfMapper interfMapper;

    /**
     * 从数据库中查询接口是否存在
     *
     * @param interfUrl           接口URL
     * @param interfRequestMethod 接口请求方法
     * @return 如果接口存在，则返回对应的接口实体；否则返回null
     */
    @Override
    public Interf getInterf(String interfUrl, String interfRequestMethod) {
        if (StringUtils.isAnyBlank(interfUrl, interfRequestMethod)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<Interf> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Interf::getInterfUrl, interfUrl)
                .eq(Interf::getInterfRequestMethod, interfRequestMethod);
        Interf interf = interfMapper.selectOne(lqw);
        return interf;
    }
}
