package com.stars.starsapibackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.stars.starsapibackend.annotation.AuthCheck;
import com.stars.starsapibackend.common.BaseResponse;
import com.stars.starsapibackend.common.ErrorCode;
import com.stars.starsapibackend.common.ResultUtils;
import com.stars.starsapibackend.exception.BusinessException;
import com.stars.starsapibackend.mapper.UserInvokeInterfMapper;
import com.stars.starsapibackend.model.vo.AnalysisInterfVO;
import com.stars.starsapibackend.service.InterfService;
import com.stars.starsapicommon.model.entity.Interf;
import com.stars.starsapicommon.model.entity.UserInvokeInterf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据分析控制器
 *
 * @author stars
 */
@RestController
@Slf4j
@RequestMapping("/analysis")
public class AnalysisController {

    // 依赖注入的UserInvokeInterfMapper实例
    @Resource
    private UserInvokeInterfMapper userInvokeInterfMapper;

    // 依赖注入的InterfService实例
    @Resource
    private InterfService interfService;

    /**
     * 获取调用次数最多的接口列表
     *
     * @return 包含分析结果的响应对象
     */
    @GetMapping("/top/interf/invoke")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<List<AnalysisInterfVO>> listTopInvokeInterf() {
        // 获取调用次数最多的用户接口列表
        List<UserInvokeInterf> userInvokeInterfList = this.userInvokeInterfMapper.listTopInvokeInterf(3);
        // 根据接口ID分组映射
        Map<Long, List<UserInvokeInterf>> interfIdObjMap =
                userInvokeInterfList.stream().collect(Collectors.groupingBy(UserInvokeInterf::getInterfId));
        QueryWrapper<Interf> queryWrapper = new QueryWrapper<>();
        // 构建查询条件，查询与接口ID匹配的接口
        queryWrapper.in("id", interfIdObjMap.keySet());
        // 查询接口列表
        List<Interf> list = this.interfService.list(queryWrapper);
        // 如果接口列表为空，抛出系统错误异常
        if (CollectionUtils.isEmpty(list)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        List<AnalysisInterfVO> collect = list.stream().map(interf -> {
            AnalysisInterfVO analysisInterfVO = new AnalysisInterfVO();
            // 复制接口信息到AnalysisInterfVO对象
            BeanUtils.copyProperties(interf, analysisInterfVO);
            // 设置总调用次数
            analysisInterfVO.setTotalInvokeNum(interfIdObjMap.get(interf.getId()).get(0).getTotalInvokeNum());
            return analysisInterfVO;
            // 构建分析结果列表
        }).collect(Collectors.toList());
        // 返回包含分析结果的成功响应
        return ResultUtils.success(collect);
    }
}
