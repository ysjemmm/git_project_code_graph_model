package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@AllArgsConstructor
public enum DataCorrectTypeEnum {
    // 主体类型: 0项目，1产品需求2业务需求
    PROJECT(0),

    PRODUCT_DEMAND(1),

    BIZ_DEMAND(2);

    private final Integer code;

}
