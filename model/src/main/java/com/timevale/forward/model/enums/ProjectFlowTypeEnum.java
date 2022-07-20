package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectFlowTypeEnum {
    INTERNAL_AUDIT(10, "需求内审"),
    DEMAND_CONSTRUE(20, "需求串讲"),
    UED_AUDIT(27, "UED评审"),
    TECHNICAL_REVIEW(30, "详设评审")
    ;
    final private Integer code;

    final private String text;
}
