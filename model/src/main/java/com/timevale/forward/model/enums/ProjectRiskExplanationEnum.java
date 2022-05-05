package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/04/26 19:10
 */
@Getter
@AllArgsConstructor
public enum ProjectRiskExplanationEnum {

    SUBMIT_FAILURE("提测质量不达标：提测失败"),

    TASK_OVERDUE("任务逾期：逾期未录入，逾期%s小时"),

    NODE_OVERDUE("项目关键节点：项目过程逾期，逾期%s天"),

    NODE_ENTRY_OVERDUE("项目关键节点：逾期未录入，逾期%s天"),

    INVALID("%s作废风险");

    private String text;
}
