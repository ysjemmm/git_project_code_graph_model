package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/23 16:59
 */
@Getter
@AllArgsConstructor
public enum BugStatusEnum {
    /**
     * bug打开
     */
    OPEN(0, "bug打开"),

    /**
     * 待修复
     */
    REPAIR(1, "待修复"),

    /**
     * 待验收
     */
    ACCEPTANCE(2, "待验收"),

    /**
     * 待确认
     */
    CONFIRM(3, "待确认"),

    /**
     * 延迟修复
     */
    POSTPONE_REPAIR(4, "延期修复"),

    /**
     * 完成
     */
    COMPLETE(5, "完成"),

    /**
     * 关闭
     */
    CLOSE(6, "关闭"),

    /**
     * 已转需求
     */
    REQUIRED(7, "已转需求");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugStatusEnum e : BugStatusEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return OPEN.text;
    }

    public static boolean canConvertBizDemand(Integer code) {
        return OPEN.getCode().equals(code) || REPAIR.getCode().equals(code) || POSTPONE_REPAIR.getCode().equals(code);
    }


    public static boolean canRelease(Integer code) {
        return COMPLETE.getCode().equals(code) || CLOSE.getCode().equals(code)
                || POSTPONE_REPAIR.getCode().equals(code) || REQUIRED.getCode().equals(code);
    }

    public static boolean completed(String text) {
        return COMPLETE.getText().equals(text) || CLOSE.getText().equals(text) || REQUIRED.getText().equals(text);
    }
}
