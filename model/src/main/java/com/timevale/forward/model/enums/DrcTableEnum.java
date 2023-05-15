package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DrcTableEnum {
    BIZ_DEMAND("biz_demand"),
    TASK("task"),
    PROJECT("project"),
    PROJECT_MILESTONE("project_milestone");

    private final String text;
}
