package com.timevale.forward.service.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 每个大域预留 4 位用于扩展权限
 * @author: 17120
 * @date: 2025/11/18 14:53
 */
@AllArgsConstructor
@Getter
public enum BizPermissionTypeEnum {

    // 所有权限
    ALL("所有权限", 0),

    // 大域 --- 产品需求相关权限
    PRODUCT_DEMAND_MODIFY("产品需求修改权限[新增、编辑]", (1 << 1) & 0xffffffffL);

    private final String name;
    private final long value;

}
