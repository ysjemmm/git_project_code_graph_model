package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2021/12/15 10:00
 */
@Getter
@Setter
public class ProjectMemberEvaluateDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 项目成员id
     */
    private String userId;
    /**
     * 项目成员名称
     */
    private String userName;
    /**
     * 计划工作量（天)
     */
    private BigDecimal planWorkload;
    /**
     * 实际工作量（天）
     */
    private BigDecimal actualWorkload;
    /**
     * 成员评价等级,0-空，10-S，20-A，30-B,40-C
     */
    private Integer evaluateGrade;
    /**
     * 成员评价说明
     */
    private String evaluateExplain;
    /**
     * 是否纳入积分统计
     */
    private Boolean includeStat;
}
