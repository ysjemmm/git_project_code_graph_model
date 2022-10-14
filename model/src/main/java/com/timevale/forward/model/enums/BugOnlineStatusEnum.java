package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 14:33
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlineStatusEnum {
    /**
     * 问题上报
     */
    PROBLEM_REPORT(0, "问题上报"),

    /**
     * 待确认
     */
    BE_CONFIRM(1, "待确认"),

    /**
     * 关闭
     */
    CLOSE(2, "关闭"),

    /**
     * 问题确认
     */
    QUESTION_CONFIRM(3, "问题确认"),

    /**
     * 问题修复
     */
    QUESTION_REPAIR(4, "问题修复"),

    /**
     * QA修复确认
     */
    REPAIR_CONFIRM(5, "QA修复确认"),

    /**
     * 待上线
     */
    ONLINE(6, "待上线"),

    /**
     * 挂起
     */
    HANG_UP(7, "挂起"),

    /**
     * 完成
     */
    COMPLETE(8, "完成"),

    /**
     * 已转需求
     */
    REQUIRED(9, "已转需求");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineStatusEnum e : BugOnlineStatusEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "error code";
    }

    public static boolean canConvertBizDemand(Integer code) {
        return HANG_UP.getCode().equals(code) || PROBLEM_REPORT.getCode().equals(code) || QUESTION_CONFIRM.getCode().equals(code);
    }

}