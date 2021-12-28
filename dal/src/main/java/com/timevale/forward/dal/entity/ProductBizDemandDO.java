package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/16 13:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductBizDemandDO extends BaseDO {
    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 业务需求id
     */
    private Long bizDemandId;


    /**
     * 需求状态
     */
    private Integer status;
}
