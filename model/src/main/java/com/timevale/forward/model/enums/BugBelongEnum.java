package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/23 18:09
 */
@Getter
@AllArgsConstructor
public enum BugBelongEnum {

    /**
     * 后端bug
     */
    BACK_END(0, "后端bug"),

    /**
     * PC客户端
     */
    PC_CLIENT(1, "PC客户端"),

    /**
     * PCweb端
     */
    PC_WEB(2, "PCweb端"),

    /**
     * Android
     */
    ANDROID(3, "Android"),

    /**
     * ios
     */
    IOS(4, "IOS"),

    /**
     * h5
     */
    H5(5, "H5");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        if (code < values().length) {
            return values()[code].text;
        }
        return "errorCode";
    }
}
