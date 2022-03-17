package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/03/17 17:32
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketReasonEnum {

    /**
     * 功能问题
     */
    FUNCTION_PROBLEM(0, "功能问题"),

    /**
     * 性能问题
     */
    PERFORMANCE_PROBLEM(1, "性能问题"),

    /**
     * 数据问题
     */
    DATA_PROBLEM(2,"数据问题"),

    /**
     * 环境问题
     */
    ENVIRONMENT_PROBLEM(3,"环境问题"),

    /**
     * 安全问题
     */
    SAFE_PROBLEM(4,"安全问题"),

    /**
     * 外部问题
     */
    EXTERNAL_PROBLEM(5,"外部问题")
    ;

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code) {
        for (TroubleTicketReasonEnum e : TroubleTicketReasonEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }


}
