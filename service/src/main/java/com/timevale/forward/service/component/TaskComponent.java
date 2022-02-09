package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface TaskComponent {
    /**
     *
     * @param taskListCondition taskListCondition
     * @param mainIds mainIds
     * @return taskVO
     */
    BaseResult<PageQueryResult<TaskVO>> page (TaskListCondition taskListCondition, List<Long> mainIds);

    /**
     *
     * @param projectId 项目id
     * @param projectStatus 项目状态
     */
    void updateStatusAsProjectStatusChange(Long projectId,Integer projectStatus,Boolean enableTask);

    /**
     *
     * @param startTime startTime
     * @param endTime endTime
     * @return 时长(h)
     */
    BigDecimal getElapsedTime(Date startTime, Date endTime);

    /**
     *
     * @param projectId 项目id
     * @param productLineIdsInProject 产品线id
     */
    void containProductLineInTask(Long projectId,List<Long> productLineIdsInProject);

}
