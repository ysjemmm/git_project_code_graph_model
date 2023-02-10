package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2022/02/23 17:14
 */
@Getter
@AllArgsConstructor
public enum TabEnum {
    /**
     * 项目路由
     */
    PROJECT_MANAGEMENT("projectManagement"),

    /**
     * 产品路由
     */
    PRODUCT_MANAGEMENT("productManagement"),

    /**
     * 业务需求路由
     */
    BUSINESS_MANAGEMENT("businessManagement"),

    /**
     * 任务路由
     */
    TASK_MANAGEMENT("taskManagement"),

    /**
     * 线下bug路由
     */
    BUG_MANAGEMENT("bugManagement"),

    /**
     * 线上bug路由
     */
    BUG_ONLINE_MANAGEMENT("mainBugManagement"),

    /**
     * 故障单路由
     */
    TROUBLE_MANAGEMENT("faultManagement"),

    /**
     * 业务需求路由
     */
    CUSTOM_MANAGEMENT("customerManagement"),

    /**
     * 人天路由
     */
    MAN_DAY_MANAGEMENT("manDayManagement"),

    /**
     * 内部项目路由
     */
    INNER_PROJECT_MANAGEMENT(""),
    ;

    private final String text;
}
