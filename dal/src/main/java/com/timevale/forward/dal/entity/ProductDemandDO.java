package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductDemandDO extends BaseDO {
    /**
     * name
     */
    private String name;
    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    private Integer priority;
    /**
     * 产品线
     */
    private Long productLineId;
    /**
     * 类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求
     */
    private String type;

    /**
     * 0待排期,10已列入项目,20项目进行中,30已完成上线,-10已暂停,-20已作废
     */
    private Integer status;
    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人
     */
    private String ownerId;
    /**
     * 描述
     */
    private String desc;

    /**
     * 预期排期时间
     */
    private Date expectScheduleTime;

    /**
     * UED资源评估（人天）
     */
    private BigDecimal uedTime;

    /**
     * 后端资源评估（人天）
     */
    private BigDecimal backTime;

    /**
     * 前端资源评估（人天）
     */
    private BigDecimal frontTime;

    /**
     * 测试资源评估（人天）
     */
    private BigDecimal qaTime;

    /**
     * 功能迁移评估（人天）
     */
    private BigDecimal transferTime;

    /**
     * 总资源评估（人天）
     */
    private BigDecimal totalTime;

}
