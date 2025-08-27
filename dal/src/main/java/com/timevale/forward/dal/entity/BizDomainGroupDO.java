package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by qiyuan
 * @date 2025/08/25 10:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDomainGroupDO extends BaseDO {

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
     * 上架状态：0-未上架，1-已上架
     */
    private Integer listingStatus;

    /**
     * 描述
     */
    private String desc;

}
