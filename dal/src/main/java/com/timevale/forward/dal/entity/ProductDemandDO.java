package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: xingyun
 * @create: 2021-12-16 16:01
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductDemandDO extends BaseDO {
    /**
     * id
     */
    private Long id;
    /**
     * name
     */
    private String name;
    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    private Byte priority;
    /**
     * 产品线
     */
    private Long productLineId;
    /**
     * 类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求
     */
    private Byte type;
    /**
     * 负责人
     */
    private String owner;
    /**
     * 描述
     */
    private String desc;

}
