package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.TaskTimeDO;

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

}
