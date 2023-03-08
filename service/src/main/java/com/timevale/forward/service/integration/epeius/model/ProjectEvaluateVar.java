package com.timevale.forward.service.integration.epeius.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2023/03/08 13:53
 */
@Getter
@Setter
public class ProjectEvaluateVar {
    /**
     * 项目考核维度
     */
    private String dimensionName;

    /**
     * 评分
     */
    private String scores;

    /**
     * 评分描述
     */
    private String scoresDesc;
}
