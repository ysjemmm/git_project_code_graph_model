package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TaskProductDemandCondition;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.TaskProductDemandDO;
import com.timevale.forward.dal.entity.TaskProductDemandUpdateDO;
import org.apache.ibatis.annotations.Param;

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
     * @param taskProductDemandCondition taskProductDemandCondition
     * @return TaskProductDemandDO
     */
    List<TaskProductDemandDO> get(TaskProductDemandCondition taskProductDemandCondition);

    /**
     *
     * @param taskId 查询条件
     * @return 项目产品需求清单
     */
    List<ProductDemandListDO> linkProductDemandList(@Param("taskId") Long taskId);

    /**
     * 新增项目-产品需求
     *
     * @param taskProductDemandUpdateDO 新增产品需求-任务
     * @return int
     */
    int update(TaskProductDemandUpdateDO taskProductDemandUpdateDO);
}
