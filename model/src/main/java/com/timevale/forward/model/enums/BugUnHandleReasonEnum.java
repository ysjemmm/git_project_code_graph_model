package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/2/24 15:39
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugUnHandleReasonEnum {
    /**
     * 被否定
     */
    BE_DENIED(0, "被否定"),

    /**
     * 重复提交
     */
    REPEAT_SUBMIT(1, "重复提交"),

    /**
     * 无法再次复现
     */
    CANNOT_REPRESENT(2, "无法再次复现"),

    /**
     * 前端缓存
     */
    FRONT_END_CACHE(3, "前端缓存"),

    /**
     * 产品需求调整
     */
    PRODUCT_DEMAND_ADJUSTMENT(4, "产品需求调整"),

    /**
     * 无
     */
    NOT(10, "无");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugUnHandleReasonEnum e : BugUnHandleReasonEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
