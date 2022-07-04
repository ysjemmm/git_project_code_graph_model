package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectNodeFlowDO;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
public interface ProjectNodeFlowComponent {

    /**
     * @param projectNodeFlowDO projectNodeFlowDO
     */
    void add(ProjectNodeFlowDO projectNodeFlowDO);

    /**
     *
     * @param oldPlanEndDate oldPlanEndDate
     * @param planEndDate planEndDate
     * @param projectNodeFlowDO projectNodeFlowDO
     */
    void process(Date oldPlanEndDate ,Date planEndDate, ProjectNodeFlowDO projectNodeFlowDO);

}
