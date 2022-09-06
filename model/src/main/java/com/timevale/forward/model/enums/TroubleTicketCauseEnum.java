package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2022/08/23 10:53
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketCauseEnum {

    CODE(0,"编码导致"),

    OTHER(10,"其它原因");

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code) {
        for (TroubleTicketCauseEnum e : TroubleTicketCauseEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }


}
