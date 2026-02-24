package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 产品需求资源人天分配表（按人拆分）
 *
 * @author kiro
 * @date 2026-02-24
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class ProductDemandOwnerTimeDO extends BaseDO {

    /**
     * 产品需求ID
     */
    private Long productDemandId;

    /**
     * 负责人账号ID
     */
    private String ownerId;

    /**
     * 负责人姓名
     */
    private String owner;

    /**
     * 资源类型: frontend/backend/qa/ued/product/ops/security
     */
    private String resourceType;

    /**
     * 分配人天
     */
    private BigDecimal resourceTime;

    /**
     * 所属规划分组ID（冗余）
     */
    private Long productDemandGroupId;

    /**
     * 业务域集ID（冗余）
     */
    private Long bizDomainGroupId;
}
