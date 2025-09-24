package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author by YangXu
 * @date 2023/06/12 11:15
 */
@Getter
@AllArgsConstructor
public enum BugOnlinePriorityEnum {
    /**
     * 低
     */
    LOW(0, "低",0),

    /**
     * 中
     */
    MIDDLE(1, "中", 96),

    /**
     * 高
     */
    HIGH(2, "高",111),

    /**
     * 紧急
     */
    URGENT(3, "紧急",120);

    private final Integer code;
    private final String text;
    private final Integer floorScore;

    public static String getTextByCode(Integer code) {
        for (BugOnlinePriorityEnum e : BugOnlinePriorityEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }

    public static BugOnlinePriorityEnum getByScore(Integer score) {
        BugOnlinePriorityEnum[] priorities = BugOnlinePriorityEnum.values();
        Arrays.sort(priorities, (a,b)-> b.floorScore.compareTo(a.floorScore));
        for (BugOnlinePriorityEnum priority : priorities) {
            if (priority.floorScore.compareTo(score) <= 0) {
                return priority;
            }
        }
        return LOW;
    }
}
