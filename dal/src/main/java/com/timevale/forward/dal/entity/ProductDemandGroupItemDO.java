package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 产品-分组关系表实体类
 * 对应表：product_demand_group_item
 * @author by qiyuan
 * @date 2025/07/15 10:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ProductDemandGroupItemDO extends BaseDO {
    // 产品需求分组id
    private Long productDemandGroupId;
    // 产品需求id
    private Long productDemandId;
    // 相对位置
    private Double position;
    // 版本号
    private Long version;
    /**
     * 虚拟列，用于唯一索引
     * 已删除的这个字段为null
     */
    private Boolean isActive;
} 