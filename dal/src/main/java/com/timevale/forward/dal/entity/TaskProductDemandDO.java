package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by xingyun
 * @date 2021/12/16 13:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskProductDemandDO extends BaseDO {

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 产品需求id
     */
    private Long productDemandId;

}
