package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author by qiyuan
 * @date 2025/08/25 10:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class BizDomainGroupRelationDO extends BaseDO {

    /**
     * 业务域id
     */
    private Long bizDomainId;


    /**
     * 业务域集id
     */
    private Long bizDomainGroupId;

}
