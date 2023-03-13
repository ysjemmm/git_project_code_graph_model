package com.timevale.forward.service.integration.epeius.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2023/03/08 13:53
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProjectEvaluateVar {
    /**
     * 项目考核维度id
     */
    private Long dimensionId;

    /**
     * 项目考核维度名称
     */
    private String dimensionName;

    /**
     * 评分
     */
    private BigDecimal scores;

    /**
     * 评分描述
     */
    private String scoresDesc;
}
