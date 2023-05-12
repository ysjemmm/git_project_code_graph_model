package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2021/12/15 10:00
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProjectEvaluateDO extends BaseDO{
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 评价维度id
     */
    private Long evaluateDimensionId;
    /**
     * 评分
     */
    private BigDecimal scores;
    /**
     * 评分描述
     */
    private String scoresDesc;
    /**
     * PMO评分
     */
    private BigDecimal pmoScores;
    /**
     * PMO评分描述
     */
    private String pmoScoresDesc;

    /**
     * 审核人评分
     */
    private BigDecimal reviewerScores;

    /**
     * 审核人评分描述
     */
    private String reviewerScoresDesc;
}
