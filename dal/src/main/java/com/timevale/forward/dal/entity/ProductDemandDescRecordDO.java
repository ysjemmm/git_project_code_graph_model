package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 产品需求描述记录表
 * TableName product_demand_desc_record
 */
@Getter
@Setter
@Accessors(chain = true)
public class ProductDemandDescRecordDO extends BaseDO {

    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 版本号
     */
    private BigDecimal version;

    /**
     * 变更后需求描述
     */
    private String desc;

}