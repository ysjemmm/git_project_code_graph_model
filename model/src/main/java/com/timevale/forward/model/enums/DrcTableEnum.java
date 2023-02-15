package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DrcTableEnum {

    TASK("task"),
    PROJECT("project"),
    PROJECT_MILESTONE("project_milestone");

    private String text;
}
