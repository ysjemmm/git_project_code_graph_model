package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/2/24 18:19
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BizChangeLogTypeEnum {
    /**
     * 项目
     */
    PROJECT(2, "项目"),

    /**
     * 产品需求
     */
    PRODUCT_DEMAND(3, "产品需求"),

    /**
     * 业务需求
     */
    BIZ_DEMAND(4, "业务需求"),

    /**
     * 客户需求
     */
    CUSTOM_DEMAND(5, "客户需求"),

    /**
     * 线下bug
     */
    BUG_OFFLINE(6, "线下bug"),

    /**
     * 线上bug
     */
    ORIGIN_BUG_ONLINE(7, "原线上bug");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BizChangeLogTypeEnum e : BizChangeLogTypeEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }
}
