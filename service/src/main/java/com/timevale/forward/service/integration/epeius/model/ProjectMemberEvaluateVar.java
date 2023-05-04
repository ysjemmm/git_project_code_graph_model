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
public class ProjectMemberEvaluateVar {
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
    private String evaluateGradeName;
    /**
     * 成员评价说明
     */
    private String evaluateExplain;
}
