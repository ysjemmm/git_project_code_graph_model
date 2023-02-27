package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/02/27 10:37
 */
@Getter
@AllArgsConstructor
public enum PersonLevelEnum {

    CORE(1,"核心成员"),

    EXTENSION(2,"扩展成员");

    private final Integer code;
    private final String text;

    public static PersonLevelEnum getByCode(Integer code) {
        PersonLevelEnum[] values = PersonLevelEnum.values();
        for (PersonLevelEnum value : values) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
