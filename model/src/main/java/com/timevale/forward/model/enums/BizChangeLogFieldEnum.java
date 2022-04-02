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

    PROJECT_STATUS("项目状态"),

    PRODUCT_DEMAND_STATUS("产品需求状态"),

    BIZ_DEMAND_STATUS("业务需求状态"),

    PD("产品经理");

    private final String text;

}
