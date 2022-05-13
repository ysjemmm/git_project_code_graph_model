package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectPublishPlanDO extends BaseDO {
    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 产品线id
     */
    private Long productLineId;


}
