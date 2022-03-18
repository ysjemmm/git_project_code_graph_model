package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 15:43
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlineRecurrentEnum {
    /**
     * 是
     */
    IS(0, "是"),

    /**
     * 否
     */
    NOT(1, "否");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineRecurrentEnum e : BugOnlineRecurrentEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "error code";
    }
}
