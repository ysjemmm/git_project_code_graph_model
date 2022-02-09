package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.TaskTimeDTO;
import com.timevale.forward.dal.entity.TaskDO;

import java.util.Date;
import java.util.List;

public interface TaskTimeComponent {
    /**
     *
     * @param taskId 任务id
     * @param endDate 结束时间
     */
    void updateEndDate(Long taskId,Date endDate);


    /**
     *
     * @param taskId 任务id
     * @param startDate 开始时间
     * @param endDate 结束时间
     */
    void insert(Long taskId,Date startDate,Date endDate);

    /**
     *
     * @param taskId 任务id
     * @param accounts 花名
     */
    List<TaskTimeDTO> getUseTime(TaskDO taskDO);

}
