package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * 产品需求描述审批流程表
 * TableName product_demand_desc_flow
 */
@Getter
@Setter
public class ProductDemandDescFlowDO extends BaseDO {

    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 流程id
     */
    private String flowId;

    /**
     * 评审状态:-1已撤回,0审核中,1审核通过,2审核不通过
     */
    private Integer status;

    /**
     * 流程阶段:0:第一阶段,1:第二阶段
     */
    private Integer stage;

    /**
     * 变更前描述
     */
    private String desc;

    /**
     * 变更后描述
     */
    private String changeDesc;

    /**
     * 变更类型:0需求调研不充分、1业务需求变更或新增、2业务需求理解偏差、3需求对现有业务流造成改动需调整方案、9其他
     */
    private Integer changeType;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 项目经理id
     */
    private String pmId;

    /**
     * 项目经理
     */
    private String pm;

    /**
     * 项目经理id
     */
    private String poId;

    /**
     * 项目经理
     */
    private String po;

}