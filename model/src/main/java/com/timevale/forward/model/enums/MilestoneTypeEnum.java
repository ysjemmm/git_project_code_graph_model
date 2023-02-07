package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Getter
@AllArgsConstructor
public enum MilestoneTypeEnum {

    TASK(0, "任务"),
    PROJECT(1, "项目");

    private final Integer code;
    private final String text;


}
