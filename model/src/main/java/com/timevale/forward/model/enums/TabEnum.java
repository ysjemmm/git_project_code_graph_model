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
    // 跳转页
    PROJECT_MANAGEMENT("projectManagement"),
    PRODUCT_MANAGEMENT("productManagement"),
    BUSINESS_MANAGEMENT("businessManagement"),
    TASK_MANAGEMENT("taskManagement"),
    BUG_MANAGEMENT("bugManagement");
    private final String text;
}
