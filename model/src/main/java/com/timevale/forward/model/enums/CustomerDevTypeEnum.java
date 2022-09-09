package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author by YangXu
 * @date 2021/12/21 14:46
 */
@Getter
@AllArgsConstructor
public enum CustomerDevTypeEnum {
    /**
     * 预期上线时间
     */
    CONTRACT(0, "基于销售合同"),

    PROJECT(1, "基于项目");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (CustomerDevTypeEnum e : CustomerDevTypeEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return StringUtils.EMPTY;
    }
}
