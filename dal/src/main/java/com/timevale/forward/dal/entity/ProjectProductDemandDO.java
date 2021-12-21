package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectProductDemandDO extends BaseDO {

    /**
     * projectId
     */
    private Long projectId;

    /**
     * productDemandId
     */
    private Long productDemandId;
    

}
