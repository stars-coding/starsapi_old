package com.stars.starsapibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stars.starsapicommon.model.entity.User;
import org.apache.ibatis.annotations.Param;

/**
 * @Entity com.stars.starsapicommon.model.entity.User
 */
public interface UserMapper extends BaseMapper<User> {

    int selectUserCount(@Param("userAccount") String paramString);
}
