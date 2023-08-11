package com.stars.starsapibackend.model.vo;

import com.stars.starsapicommon.model.entity.Interf;
import lombok.Data;

/**
 * 分析接口视图
 *
 * @author stars
 */
@Data
public class AnalysisInterfVO extends Interf {

    /**
     * 总计调用次数
     */
    private Long totalInvokeNum;

    private static final long serialVersionUID = 1L;
}
