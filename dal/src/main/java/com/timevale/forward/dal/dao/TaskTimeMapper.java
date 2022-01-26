package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.TaskTimeDO;
import org.apache.ibatis.annotations.Param;

/**
 * @author by xingyun
 * @date 2021/12/15 11:40
 */
public interface TaskTimeMapper {


    /**
     * 新增任务耗时表
     *
     * @param taskTimeDO 任务
     * @return int
     */
    int insert(TaskTimeDO taskTimeDO);

    /**
     * 新增任务耗时表
     *
     * @param taskId 任务
     * @return int
     */
    int delete(@Param("taskId") Long taskId);

    /**
     * 查看任务耗时表
     *
     * @param taskId 任务
     * @return int
     */

    TaskTimeDO get(@Param("taskId") Long taskId);

    /**
     * 更新任务耗时表
     *
     * @param taskTimeDO 任务
     */
    void update(TaskTimeDO taskTimeDO);

}
