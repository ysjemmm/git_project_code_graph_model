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
public class ProductDemandGroupFieldDO extends BaseDO {

    /**
     * 需求id
     */
    private Long id;

    /**
     * 业务域id
     */
    private Long bizDomainId;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 标签类别id
     */
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

    /**
     * 类型
     */
    private String type;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 需求排期时间
     */
    private String expectScheduleTime;

    /**
     * 负责人id
     */
    private String ownerId;

    /**
     * 总数
     */
    private Long total;
}
