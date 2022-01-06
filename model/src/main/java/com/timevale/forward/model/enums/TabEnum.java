package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum TabEnum {
    // 跳转页
    PROJECT_EDIT("projectEdit"),
    PRODUCT_EDIT("productEdit"),
    BUSINESS_EDIT("businessEdit");
    private String text;

    TabEnum(String text){this.text = text;}
}
