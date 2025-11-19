package com.timevale.forward.service.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 权限作用域枚举
 * @author: 17120
 * @date: 2025/11/18 14:53
 */
@AllArgsConstructor
@Getter
public enum BizPermissionScopeEnum {

    // 所有作用域
    ALL("所有作用域"),

    // 产品线
    PRODUCT_LINE_SCOPE("产品线作用域");

    private final String name;

}
