package com.stars.starsapibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stars.starsapicommon.model.entity.Interf;
import com.stars.starsapicommon.model.vo.InterfVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity com.stars.starsapicommon.model.entity.Interf
 */
public interface InterfMapper extends BaseMapper<Interf> {

    List<InterfVO> selectMyInterfByPage(
            @Param("userId") long userId,
            @Param("start") int start,
            @Param("pageSize") long pageSize,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder,
            @Param("interfDescription") String interfDescription);

    int selectMyInterfCount(long userId);
}
