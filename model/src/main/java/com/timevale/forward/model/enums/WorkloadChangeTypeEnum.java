package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/03/09 17:57
 */
@Getter
@AllArgsConstructor
public enum WorkloadChangeTypeEnum {
    /**
     * 计划总工作量增加
     */
    PLAN_WORKLOAD_ADD("计划总工作量增加"),

    /**
     * 积分总工作量增加
     */
    POINTS_WORKLOAD_ADD("积分总工作量增加");

    private final String text;
}
