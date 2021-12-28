package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDomainDO extends BaseDO{

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

}
