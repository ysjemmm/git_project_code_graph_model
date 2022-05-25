package com.timevale.forward.service.component;

import java.util.List;

public interface ProjectPublishPlanComponent {

    /**
     *
     * @param list 发布计划id
     * @param projectId 项目id
     */
    void add(List<Long> list,Long projectId);


    /**
     *
     * @param publishPlanId 发布计划id
     * @param projectId 项目id
     */
    void update(Long publishPlanId,Long projectId);

    /**
     *
     * @param projectId projectId
     * @return boolean
     */
    boolean anyMatchNotFinished(Long projectId);


    /**
     *
     * @param projectId projectId
     * @return boolean
     */
    boolean linkPublishPlan(Long projectId);

}
