package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 15:23
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlinePriorityEnum {
    /**
     * 低
     */
    LOW(0, "低"),

    /**
     * 中
     */
    MIDDLE(1, "中"),

    /**
     * 高
     */
    HIGH(2, "高"),

    /**
     * 紧急
     */
    URGENT(3, "紧急");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlinePriorityEnum e : BugOnlinePriorityEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "error code";
    }
}
