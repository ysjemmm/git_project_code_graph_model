package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TaskProductDemandCondition;
import com.timevale.forward.dal.entity.TaskProductDemandDO;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/16 14:06
 */
public interface TaskProductDemandMapper {


    /**
     * 新增项目-产品需求
     *
     * @param taskProductDemandDO 新增产品需求-任务
     * @return int
     */
    int batchInsert(List<TaskProductDemandDO> taskProductDemandDO);

    /**
     *
     * @param taskProductDemandCondition
     * @return
     */
    List<TaskProductDemandDO> get(TaskProductDemandCondition taskProductDemandCondition);
}
