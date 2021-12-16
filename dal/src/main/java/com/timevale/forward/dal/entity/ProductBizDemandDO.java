package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @Date 2021/12/16 13:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductBizDemandDO extends BaseDO {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 业务需求id
     */
    private Long bizDemandId;
}
