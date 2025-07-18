package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 产品分组的需求项
 * @author by qiyuan
 * @date 2025/07/15 10:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ProductDemandGroupItemListDO extends ProductDemandGroupItemDO {

    /**
     * name
     */
    private String name;

    /**
     * 需求状态:0待排期,10已列入项目,20项目进行中,30已完成上线,-10已暂停,-20已作废
     */
    private Integer status;

    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    private Integer priority;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人ID
     */
    private String ownerId;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 产品线
     */
    private String productLineName;

    /**
     * 业务域
     */
    private String bizDomainName;


    /**
     * 抄送人
     */
    private String copier;

    /**
     * 类型
     */
    private String type;

    /**
     * 预期排期时间
     */
    private Date expectScheduleTime;

    /**
     * 产品需求创建时间
     */
    private Date demandCreateDate;
    /**
     * 产品需求修改时间
     */
    private Date demandModifyDate;

    /**
     * 产品需求创建人id
     */
    private String demandCreateManId;

    /**
     * 产品需求创建人
     */
    private String demandCreateMan;

} 