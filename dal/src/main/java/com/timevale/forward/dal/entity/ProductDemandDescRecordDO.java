package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 产品需求描述记录表
 * TableName product_demand_desc_record
 */
@Getter
@Setter
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

    /**
     * 审批通过操作人id
     */
    private String operatorId;

    /**
     * 审批通过操作人
     */
    private String operator;

}