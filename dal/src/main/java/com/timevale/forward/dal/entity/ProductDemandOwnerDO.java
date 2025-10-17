package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author mayang
 * @date 2025-10-15 16:58
 **/
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Data
public class ProductDemandOwnerDO extends BaseDO {
    /**
     * id
     */
    private Long id;
    /**
     * 产品需求id
     */
    @EqualsAndHashCode.Include
    private Long productDemandId;

    /**
     * 关联负责人
     */
    private String owner;
    /**
     * 关联负责人字符串id
     */
    @EqualsAndHashCode.Include
    private String ownerId;

    /**
     * 资源类型
     */
    @EqualsAndHashCode.Include
    private String resourceType;

}
