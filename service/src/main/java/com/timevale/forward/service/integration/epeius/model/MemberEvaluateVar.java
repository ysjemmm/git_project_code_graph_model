package com.timevale.forward.service.integration.epeius.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2023/06/19 18:08
 */
@Getter
@Setter
public class MemberEvaluateVar {
    /**
     * 项目成员
     */
    private String userName;
    /**
     * 调整前-计划工作量
     */
    private String planWorkloadBefore;
    /**
     * 调整后-计划工作量
     */
    private String planWorkloadAfter;
    /**
     * 是否纳入积分
     */
    private String includeStat;
}
