package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/16 13:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskProductDemandUpdateDO extends BaseDO {

    /**
     * 更新时任务id
     */
    private List<Long> updateTaskIds;

    /**
     * 产品需求id
     */
    private Long productDemandId;

}
