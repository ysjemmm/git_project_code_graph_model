package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/23 13:59
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDemandLinkProductDemandListDO extends BaseDO {

    /**
     * 产品需求主题
     */
    private String name;

    /**
     * 产品需求优先级
     */
    private Integer priority;

    /**
     * 业务域id
     */
    private Long bizDomainId;

    /**
     * 业务域名称
     */
    private String bizDomainName;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 产品线名称
     */
    private String productLineName;

    /**
     * 产品需求负责人
     */
    private String owner;

    /**
     * 产品需求负责人
     */
    private String ownerId;

    /**
     * 产品需求状态
     */
    private Integer status;
}
