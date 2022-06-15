package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 15:18
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlineBeloneEnum {
    /**
     * 后端bug
     */
    BACK_END(0, "后端bug"),

    /**
     * 1PC客户端
     */
    CUSTOMER(1, "PC客户端"),

    /**
     * PCweb端
     */
    WEB(2, "PCweb端"),

    /**
     * Android
     */
    Android(3, "Android"),

    /**
     * IOS
     */
    IOS(4, "IOS"),

    /**
     * H5
     */
    H5(5, "H5"),

    /**
     * 微信小程序
     */
    WECHAT_MINI_PROGRAMS(6,"微信小程序"),

    /**
     * 支付宝小程序
     */
    ALIPAY_MINI_PROGRAMS(7,"支付宝小程序");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineBeloneEnum e : BugOnlineBeloneEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "error code";
    }
}
