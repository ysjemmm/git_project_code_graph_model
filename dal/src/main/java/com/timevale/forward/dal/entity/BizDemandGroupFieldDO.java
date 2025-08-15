package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @auther: yuhua
 * @date: 2025/7/21 11:31
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDemandGroupFieldDO extends BaseDO {

    private Integer priority;

    private Long bizDomainId;

    private Long productLineId;

    private Long subProductLineId;

    private Integer status;

    private String receiveManId;

    private String receiveManIds;

    private Long deptId;

    private Long labelCategoryId;

    private String labelCategoryIds;

    private String targetCustomer;

    /**
     * 总数
     */
    private Long total;
}
