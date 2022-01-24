package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

/**
 * @author by xingyun
 * @date 2021/12/16 14:07
 */
@Data
@Builder
public class TaskProductDemandCondition {
    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 任务id
     */
    private Long taskId;

}
