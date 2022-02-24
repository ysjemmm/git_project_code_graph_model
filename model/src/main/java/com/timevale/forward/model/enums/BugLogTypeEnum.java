package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/2/24 18:19
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugLogTypeEnum {
    /**
     * 线下bug
     */
    MUST_HAPPEN(0, "线下bug"),

    /**
     * 线上bug
     */
    OCCASIONALLY(1, "线上bug");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugLogTypeEnum e : BugLogTypeEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }
}
