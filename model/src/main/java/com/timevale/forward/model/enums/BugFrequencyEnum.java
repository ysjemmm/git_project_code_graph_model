package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/2/24 15:35
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugFrequencyEnum {
    /**
     * 必现
     */
    MUST_HAPPEN(0, "必现"),

    /**
     * 偶现
     */
    OCCASIONALLY(2, "偶现");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugFrequencyEnum e : BugFrequencyEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }
}