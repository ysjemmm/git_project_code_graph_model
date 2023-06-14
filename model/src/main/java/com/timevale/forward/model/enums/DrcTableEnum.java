package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DrcTableEnum {
    BIZ_DEMAND("biz_demand"),
    TASK("task"),
    PROJECT("project"),
    BUG_ONLINE("bug_online"),
    PROJECT_MILESTONE("project_milestone"),
    PROJECT_MILESTONE_ACTION("project_milestone_action");

    private final String text;

    public static DrcTableEnum getByText(String text) {
        for (DrcTableEnum value : DrcTableEnum.values()) {
            if (value.text.equals(text)) {
                return value;
            }
        }
        return null;
    }
}
