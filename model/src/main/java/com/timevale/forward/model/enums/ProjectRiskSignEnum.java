package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/04/26 19:10
 */
@Getter
@AllArgsConstructor
public enum ProjectRiskSignEnum {

    SUBMIT_FAILURE("提测失败"),

    TASK_OVERDUE("逾期时间%s天%小时"),

    NODE_OVERDUE("逾期时间%s天");

    private String text;
}
