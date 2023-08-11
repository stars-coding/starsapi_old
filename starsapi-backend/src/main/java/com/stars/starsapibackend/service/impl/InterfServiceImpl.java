package com.stars.starsapibackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stars.starsapibackend.common.ErrorCode;
import com.stars.starsapibackend.common.PageHelper;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.mapper.InterfMapper;
import com.stars.starsapibackend.model.dto.interf.InterfQueryRequest;
import com.stars.starsapibackend.service.InterfService;
import com.stars.starsapibackend.service.UserInvokeInterfService;
import com.stars.starsapibackend.service.UserService;
import com.stars.starsapicommon.model.entity.Interf;
import com.stars.starsapicommon.model.vo.InterfVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 接口服务实现
 * 提供接口信息的管理和查询功能，包括接口验证和分页查询等操作。
 *
 * @author stars
 */
@Service
public class InterfServiceImpl extends ServiceImpl<InterfMapper, Interf> implements InterfService {

    @Resource
    private UserService userService;

    @Resource
    private UserInvokeInterfService userInvokeInterfService;

    @Resource
    private InterfMapper interfMapper;

    /**
     * 验证接口信息
     * 根据添加或更新操作验证接口信息的有效性。
     *
     * @param interf 接口信息
     * @param add    是否为添加操作
     */
    @Override
    public void validInterf(Interf interf, boolean add) {
        if (interf == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String name = interf.getInterfName();
        if (add && StringUtils.isBlank(name))
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        if (StringUtils.isAnyBlank(name) && name.length() > 50)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
    }

    /**
     * 获取我的接口列表
     * 根据查询请求获取当前用户的接口列表并进行分页。
     *
     * @param interfQueryRequest 查询请求
     * @return 接口列表的分页信息
     */
    @Override
    public PageHelper<InterfVO> getMyInterf(InterfQueryRequest interfQueryRequest) {

        int pageSize = interfQueryRequest.getPageSize();
        int pageNum = interfQueryRequest.getCurrent();
        long userId = interfQueryRequest.getInterfUserId();
        int count = interfMapper.selectMyInterfCount(userId);
        int pageCount = (count % pageSize == 0) ? (count / pageSize) : (count / pageSize + 1);
        int start = (pageNum - 1) * pageSize;
        String sortField = interfQueryRequest.getSortField();
        String sortOrder = interfQueryRequest.getSortOrder();
        if (StrUtil.isBlank(sortField))
            sortField = "i.id";
        String description = interfQueryRequest.getInterfDescription();
        description = "%" + description + "%";
        List<InterfVO> interfVOS = interfMapper.selectMyInterfByPage(userId, start, pageSize, sortField, sortOrder, description);
        PageHelper<InterfVO> interfVOPageHelper = new PageHelper<>(count, pageCount, interfVOS);
        return interfVOPageHelper;
    }
}
