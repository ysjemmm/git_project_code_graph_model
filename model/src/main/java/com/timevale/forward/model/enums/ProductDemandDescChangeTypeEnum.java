package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author jingchun
 * create on 2022/7/4
 */
@Getter
@AllArgsConstructor
public enum ProductDemandDescChangeTypeEnum {
    INSUFFICIENT_SURVEY(0, "需求调研不充分"),
    DEMAND_CHANGE(1, "需求变更"),
    MISAPPREHENSION(2, "理解偏差"),
    BROKEN_FLOW(3, "需求对现有流程造成改动"),
    ELSE(9, "其他");
    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (ProductDemandDescChangeTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }

}
