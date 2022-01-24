package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

public interface TaskComponent {
    /**
     *
     * @param taskListCondition taskListCondition
     * @param mainIds mainIds
     * @return taskVO
     */
    BaseResult<PageQueryResult<TaskVO>> page (TaskListCondition taskListCondition, List<Long> mainIds);

}
