package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/25 15:21
 */
@Getter
@AllArgsConstructor
public enum BizChangeLogFieldEnum {
    /**
     * 产品线
     */
    PRODUCT_LINE("产品线"),

    PD("产品经理");

    private final String text;

}
