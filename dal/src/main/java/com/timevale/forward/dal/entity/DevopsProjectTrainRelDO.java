package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产研项目与批量发布关联表 DO
 *
 * @author system
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

    /**
     * 删除标记：0-未删除，1-已删除
     */
    private Boolean isDeleted;

    /**
     * 创建人id
     */
    private String createManId;

    /**
     * 创建人
     */
    private String createMan;

    /**
     * 修改人id
     */
    private String modifyManId;

    /**
     * 修改人
     */
    private String modifyMan;

    // ==================== 构造方法 ====================

    public DevopsProjectTrainRelDO() {
    }

    public DevopsProjectTrainRelDO(Long projectId, Integer publishTrainId) {
        this.projectId = projectId;
        this.publishTrainId = publishTrainId;
        this.isDeleted = false;
    }
}
