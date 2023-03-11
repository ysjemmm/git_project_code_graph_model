package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 等级枚举
 *
 * @author yangxu
 * @date 2023/03/11
 */
@Getter
@AllArgsConstructor
public enum GradeEnum {
    NONE(0, ""),

    S(10, "S"),

    A(20, "A"),

    B(30, "B"),

    C(40, "C");

    private final Integer code;
    private final String text;
}
