package com.timevale.forward.service.component;

import java.util.Date;

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

}
