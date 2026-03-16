package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 线下Bug分组字段DO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOfflineGroupFieldDO extends BaseDO {

    /**
     * bug id
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
     * 状态
     */
    private Integer status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 严重程度
     */
    private Integer severity;

    /**
     * 经办人id
     */
    private String operatorId;

    /**
     * 提出人id
     */
    private String proposerId;

    /**
     * 总数
     */
    private Long total;
}
