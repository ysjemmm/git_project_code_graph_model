package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/25 15:21
 */
@Getter
@AllArgsConstructor
public enum BugLogFieldEnum {
    /**
     * 关联项目
     */
    PROJECT_ID("关联项目"),

    /**
     * 产品线
     */
    PRODUCT_LINE("产品线"),

    /**
     * 状态
     */
    STATUS("状态"),

    /**
     * 原因
     */
    REASON("bug原因"),

    /**
     * bug产生原因
     */
    CAUSE("bug产生原因"),

    /**
     * 解决方案
     */
    SOLVE_PLAN("解决方案"),

    /**
     * 模块
     */
    MODEL("模块")
    ;


    private final String text;

}
