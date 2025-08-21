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

    private Long deptId;

    private Long labelId;

    /**
     * 标签类别id1
     */
    private Long labelId1;

    /**
     * 标签类别id2
     */
    private Long labelId2;

    /**
     * 标签类别id3
     */
    private Long labelId3;

    /**
     * 标签类别id4
     */
    private Long labelId4;

    /**
     * 标签类别id5
     */
    private Long labelId5;

    private Long labelCategoryId;

    private String targetCustomer;

    /**
     * 总数
     */
    private Long total;
}
