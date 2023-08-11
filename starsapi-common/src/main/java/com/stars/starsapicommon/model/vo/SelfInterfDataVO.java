package com.stars.starsapicommon.model.vo;

import lombok.Data;

/**
 * 安全接口数据视图
 *
 * @author stars
 */
@Data
public class SelfInterfDataVO {

    /**
     * 接口名称
     */
    private String interfName;

    /**
     * 用户调用接口-总计调用次数
     */
    private Long totalInvokeNum;

    /**
     * 用户调用接口-剩余调用次数
     */
    private Long leftInvokeNum;
}
