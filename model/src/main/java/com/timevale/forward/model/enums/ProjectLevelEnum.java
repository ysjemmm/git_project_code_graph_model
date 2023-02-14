package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/06/27 15:14
 */
@Getter
@AllArgsConstructor
public enum ProjectLevelEnum {

    COMMON(0,"普通项目"),

    IMPORTANT(10,"重点项目"),
    S(20, "S"),
    A(30, "A"),
    B(40, "B");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code){
        for (ProjectLevelEnum e : ProjectLevelEnum.values()) {
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
