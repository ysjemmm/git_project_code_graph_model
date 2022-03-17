package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/03/17 17:32
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketTypeEnum {

    /**
     * 业务故障
     */
    BIZ_TROUBLE(0, "业务故障"),

    /**
     * 数据故障
     */
    DATA_TROUBLE(1, "数据故障")
    ;

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code) {
        for (TroubleTicketTypeEnum e : TroubleTicketTypeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }


}
