package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产研项目与批量发布关联表 DO
 *
 * @author dijiu
 * @date 2025/09/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DevopsProjectTrainRelDO extends BaseDO {
    /**
     * 产研项目id
     */
    private Long projectId;

    /**
     * 批量发布id
     */
    private Integer publishTrainId;
}
