package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/2/24 15:26
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugPriorityEnum {
    /**
     * 紧急
     */
    URGENT(0, "紧急"),

    /**
     * 高
     */
    HIGH(10, "高"),

    /**
     * 中
     */
    MIDDLE(20, "中"),

    /**
     * 低
     */
    LOW(30, "低");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugPriorityEnum e : BugPriorityEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }
}
