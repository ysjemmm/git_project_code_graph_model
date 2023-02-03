package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/02/03 11:41
 */
@Getter
@AllArgsConstructor
public enum ProjectCategoryEnum {
    PRODUCT_PROJECT(0,"产研项目"),
    INNER_PROJECT(1,"内部项目");

    private final Integer code;
    private final String text;
}
