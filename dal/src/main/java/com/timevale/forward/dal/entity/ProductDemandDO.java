package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
    private Integer type;

    /**
     * 0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废
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


}
