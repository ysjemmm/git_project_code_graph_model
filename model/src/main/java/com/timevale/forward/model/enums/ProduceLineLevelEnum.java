package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/06/09 17:40
 */
@Getter
@AllArgsConstructor
public enum ProduceLineLevelEnum {
    NONE(0, "无", 10),
    CORE(1, "核心产品线", 15),
    COMMON(2, "一般产品线", 10),
    DELISTING(3, "即将退市产品线", 5);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static String getTextByCode(Integer code) {
        return Optional.ofNullable(getByCode(code))
                .map(ProduceLineLevelEnum::getText)
                .orElse("");
    }

    public static ProduceLineLevelEnum getByCode(Integer code) {
        for (ProduceLineLevelEnum value : ProduceLineLevelEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
