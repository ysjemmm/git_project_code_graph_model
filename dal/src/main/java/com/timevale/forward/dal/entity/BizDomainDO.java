package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDomainDO extends BaseDO {

    /**
     * 业务域名称
     */
    private String name;

    /**
     * 业务域负责人
     */
    private String owner;

    /**
     * 业务域负责人id
     */
    private String ownerId;

    /**
     * 业务域技术负责人
     */
    private String techOwner;

    /**
     * 业务域技术负责人id
     */
    private String techOwnerId;

    /**
     * PBU负责人
     */
    private String pbuOwner;

    /**
     * PBU负责人id
     */
    private String pbuOwnerId;

    /**
     * 业务需求产品线负责人是否可以直接驳回
     */
    private Boolean directReject;

    /**
     * 线上bug是否可以直接转产品需求
     */
    private Boolean directConvertBiz;

}
