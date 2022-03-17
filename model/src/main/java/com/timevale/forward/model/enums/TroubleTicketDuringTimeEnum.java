package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/03/17 17:32
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketDuringTimeEnum {

    /**
     * ≤ 5分钟
     */
    LESS_THAN_EQUAL_FIVE(0, "≤ 5分钟"),

    /**
     * 5~20分钟
     */
    FIVE_TO_TWENTY(1,"5~20分钟"),

    /**
     * 20~30分钟
     */
    TWENTY_TO_THIRTY(2,"20~30分钟"),

    /**
     * 30~60分钟
     */
    THIRTY_TO_SIXTY(3,"30~60分钟"),

    /**
     * ＞60分钟
     */
    GREATER_THEN_SIXTY(4,"＞60分钟");



    private Integer code;
    private String text;

    public static String getTextByCode(Integer code) {
        for (TroubleTicketDuringTimeEnum e : TroubleTicketDuringTimeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }


}
