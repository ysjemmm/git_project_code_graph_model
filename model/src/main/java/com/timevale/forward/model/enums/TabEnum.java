package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum TabEnum {
    // 跳转页
    PROJECT_MANAGEMENT("projectManagement"),
    PRODUCT_MANAGEMENT("productManagement"),
    BUSINESS_MANAGEMENT("businessManagement"),
    TASK_MANAGEMENT("taskManagement");
    private String text;

    TabEnum(String text){this.text = text;}
}
