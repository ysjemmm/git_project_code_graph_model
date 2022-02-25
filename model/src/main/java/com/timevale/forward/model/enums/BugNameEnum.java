package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @Date 2022/2/25 14:45
 * @Author 望轩
 */
@Getter
public enum BugNameEnum {

    BUG_OFFLINE("线下bug");

    private final String text;

    BugNameEnum(String text) {
        this.text = text;
    }
}
