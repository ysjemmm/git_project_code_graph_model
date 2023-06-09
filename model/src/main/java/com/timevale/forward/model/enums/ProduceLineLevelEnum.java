package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/06/09 17:40
 */
@Getter
@AllArgsConstructor
public enum ProduceLineLevelEnum {
    NONE(0,"无"),
    CORE(0,"核心产品线"),
    DELISTING(0,"即将退市产品线"),
    COMMON(0,"一般产品线");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (ProduceLineLevelEnum value : ProduceLineLevelEnum.values()) {
            if (value.code.equals(code)) {
                return value.text;
            }
        }
        return "";
    }
}
