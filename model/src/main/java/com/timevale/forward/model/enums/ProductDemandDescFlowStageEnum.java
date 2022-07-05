package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author jingchun
 * create on 2022/7/5
 */

@Getter
@AllArgsConstructor
public enum ProductDemandDescFlowStageEnum {
    FIRST_STAGE(0, "第一阶段"),
    SECOND_STAGE(1, "第二阶段");
    private final Integer code;
    private final String text;

}
