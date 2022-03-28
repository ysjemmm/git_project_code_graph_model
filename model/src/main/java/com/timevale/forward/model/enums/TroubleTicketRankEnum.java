package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/03/17 17:32
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketRankEnum {
    /**
     * 未达到级别
     */
    UN_RATINGS(-10, "未达到级别"),

    /**
     * P0
     */
    P0(0, "P0"),

    /**
     * P1
     */
    P1(10, "P1"),

    /**
     * P2
     */
    P2(20, "P2"),

    /**
     * P3
     */
    P3(30, "P3")
    ;

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code) {
        for (TroubleTicketRankEnum e : TroubleTicketRankEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
