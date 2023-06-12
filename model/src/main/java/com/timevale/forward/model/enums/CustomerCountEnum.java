package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/06/09 17:38
 */
@Getter
@AllArgsConstructor
public enum CustomerCountEnum {
    NONE(0, "无", 0),
    ONE(1, "单客户", 5),
    GEQ_TWO(2, "2家或2家以上客户", 10);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static CustomerCountEnum getByCode(Integer code) {
        for (CustomerCountEnum value : CustomerCountEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static String getByTextCode(Integer code) {
        return Optional.ofNullable(getByCode(code))
                .map(CustomerCountEnum::getText)
                .orElse("");
    }
}
