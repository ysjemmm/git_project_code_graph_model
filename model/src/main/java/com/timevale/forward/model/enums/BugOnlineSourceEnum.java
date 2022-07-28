package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/07/28 17:04
 */
@Getter
@AllArgsConstructor
public enum BugOnlineSourceEnum {

    FORWARD("forward", "产研系统"),

    SUPPORT("support", "运营支撑平台"),

    DUTY("duty", "值班反馈")

    ;

    private String code;
    private String text;

    public static String getTextByCode(String code) {
        for (BugOnlineSourceEnum e : BugOnlineSourceEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
