package com.stars.starsapibackend.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stars.starsapibackend.common.ErrorCode;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.service.UserInvokeInterfService;
import com.stars.starsapicommon.model.entity.UserInvokeInterf;
import com.stars.starsapicommon.service.InnerUserInvokeInterfService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 内部用户调用接口服务实现
 * 该服务提供了统计用户调用接口的次数和判断是否具有剩余调用次数的功能。
 *
 * @author stars
 */
@DubboService
public class InnerUserInvokeInterfServiceImpl implements InnerUserInvokeInterfService {


    @Autowired
    private UserInvokeInterfService userInvokeInterfService;

    /**
     * 统计用户调用接口的次数
     *
     * @param userId   用户ID
     * @param interfId 接口ID
     * @return 如果统计成功，返回true；否则返回false
     */
    @Override
    public boolean invokeCount(long userId, long interfId) {
        return userInvokeInterfService.invokeCount(userId, interfId);
    }

    /**
     * 判断是否具有剩余调用次数
     *
     * @param userId   用户ID
     * @param interfId 接口ID
     * @return 如果具有剩余调用次数，返回true；否则返回false
     */
    @Override
    public boolean validLeftNum(Long userId, Long interfId) {
        LambdaQueryWrapper<UserInvokeInterf> lqw = new LambdaQueryWrapper<>();
        lqw.eq(UserInvokeInterf::getUserId, userId)
                .eq(UserInvokeInterf::getInterfId, interfId);
        UserInvokeInterf userInvokeInterf = userInvokeInterfService.getOne(lqw);
        if (userInvokeInterf == null || userInvokeInterf.getLeftInvokeNum() <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return true;
    }
}
