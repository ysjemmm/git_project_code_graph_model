package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectProductLineBizDomain extends BaseDO {
    /**
     * projectId
     */
    private Long projectId;

    /**
     * productLineName
     */
    private String productLineName;
    /**
     * bizDomainName
     */
    private String bizDomainName;

}
