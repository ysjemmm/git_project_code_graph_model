package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2021/12/15 10:00
 */
@Getter
@Setter
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
    private Integer scores;
    /**
     * 评分描述
     */
    private String scoresDesc;
    /**
     * PMO评分
     */
    private Integer pmoScores;
    /**
     * PMO评分描述
     */
    private String pmoScoresDesc;
}
