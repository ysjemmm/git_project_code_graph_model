package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/03/17 17:32
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketInfluenceScopeEnum {

    /**
     * 全网
     */
    ALL(0, "全网"),

    /**
     * 单业务域
     */
    SINGLE_BIZ_DOMAIN(1, "单业务域"),

    /**
     * 跨多个业务域
     */
    MANY_BIZ_DOMAIN(2, "跨多个业务域")
    ;

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code) {
        for (TroubleTicketInfluenceScopeEnum e : TroubleTicketInfluenceScopeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }


}
