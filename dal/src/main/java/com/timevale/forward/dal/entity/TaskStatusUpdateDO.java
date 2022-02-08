package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskStatusUpdateDO extends BaseDO {
    /**
     * projectId
     */
    private Long projectId;
    /**
     * 更新前状态
     */
    private List<Integer> preUpdate;
    /**
     * 需要更新的状态
     */
    private Integer updated;


}
