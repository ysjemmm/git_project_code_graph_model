package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/06/12 11:19
 */
@Getter
@AllArgsConstructor
public enum CustomerGradeEnum {
    S(10,"S", 30),
    A(20,"A", 20),
    B(30,"B", 15),
    C(40,"C", 10),
    OTHER(50,"", 5);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static CustomerGradeEnum getByCode(Integer code) {
        for (CustomerGradeEnum value : CustomerGradeEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static CustomerGradeEnum getByText(String text) {
        for (CustomerGradeEnum value : CustomerGradeEnum.values()) {
            if (value.text.equals(text)) {
                return value;
            }
        }
        return null;
    }
}
