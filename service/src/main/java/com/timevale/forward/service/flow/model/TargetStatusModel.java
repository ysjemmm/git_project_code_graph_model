package com.timevale.forward.service.flow.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class TargetStatusModel {
    /**
     * 目标状态
     */
    private Integer targetStatus;

    /**
     * 作废原因
     */
    private String invalidReason;
}
