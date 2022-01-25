package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.TaskCondition;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.entity.TaskDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 11:40
 */
public interface TaskMapper {


    /**
     * 选择通过产品需求id列表
     *
     * @param condition 产品需求id列表
     * @return ProjectDO
     */
    List<TaskDO> list(TaskListCondition condition);

    /**
     * 获取任务信息
     * @param ids 任务
     * @param projectIds 项目
     * @return list
     */
    List<Long> getByProjectIds(@Param("ids") List<Long> ids,@Param("projectIds") List<Long> projectIds);

    /**
     *
     * @param ids 任务id
     * @param productLineIds 产品线id
     * @return 任务id
     */

    List<Long> getByProductLineIds(@Param("ids") List<Long> ids,@Param("productLineIds") List<Long> productLineIds);

    /**
     *
     * @param ids 任务id
     * @return 项目id
     */
    List<Long> getProjectIds(@Param("ids") List<Long> ids);


    /**
     * 新增任务信息
     *
     * @param taskDO 任务
     * @return int
     */
    int insert(TaskDO taskDO);

    /**
     *
     * @param taskCondition 查询条件
     * @return TaskDO
     */

    TaskDO get(TaskCondition taskCondition);

    /**
     * 新增任务信息
     *
     * @param taskDO 任务
     * @return int
     */
    int update(TaskDO taskDO);

}
