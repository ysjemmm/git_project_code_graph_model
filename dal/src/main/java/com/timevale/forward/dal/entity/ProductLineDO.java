package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductLineDO extends BaseDO {

    /**
     * 业务域id
     */
    private Long bizDomainId;

    /**
     * 产品线名称
     */
    private String name;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人id
     */
    private String ownerId;

    /**
     * 产品线bug负责人
     */
    private String bugOnlineOwner;

    /**
     * 产品线bug负责人id
     */
    private String bugOnlineOwnerId;

    /**
     * sr专家
     */
    private String srExpert;

    /**
     * sr专家id
     */
    private String srExpertId;

    /**
     * 产品线等级
     */
    private Integer productLineLevel;
}
