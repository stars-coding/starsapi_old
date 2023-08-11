package com.stars.starsapibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stars.starsapicommon.model.entity.UserInvokeInterf;

import java.util.List;

/**
 * @Entity com.stars.starsapicommon.model.entity.UserInvokeInterf
 */
public interface UserInvokeInterfMapper extends BaseMapper<UserInvokeInterf> {

    List<UserInvokeInterf> listTopInvokeInterf(int paramInt);
}
