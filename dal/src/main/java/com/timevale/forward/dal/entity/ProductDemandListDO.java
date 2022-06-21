package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductDemandListDO extends BaseDO {
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

}
