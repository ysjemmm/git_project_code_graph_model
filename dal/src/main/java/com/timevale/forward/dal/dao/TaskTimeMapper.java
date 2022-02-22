package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.TaskTimeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
     * @param taskIds 任务
     * @return int
     */
    int delete(@Param("taskIds") List<Long> taskIds,@Param("id") Long id);

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

    /**
     * 查看任务耗时表
     *
     * @param taskId 任务
     * @return int
     */

    List<TaskTimeDO> list(@Param("taskId") Long taskId);

    /**
     * 更新任务耗时表
     *
     * @param taskTimeDO 任务
     */
    void updateById(TaskTimeDO taskTimeDO);

//   i /**
//     * 新增任务耗时表
//     *
//     * @param taskIds 任务
//     * @return int
//     */
//    int delete(@Param("taskIds") Lst<Long> taskIds);

}
