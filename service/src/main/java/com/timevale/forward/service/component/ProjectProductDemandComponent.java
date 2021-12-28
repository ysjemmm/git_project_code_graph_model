package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectProductDemandDO;

import java.util.List;

public interface ProjectProductDemandComponent {
    /**
     *
     * @param projectProductDemandDO 项目id
     */
    void update(ProjectProductDemandDO projectProductDemandDO);

    /**
     * 新增项目-产品需求
     *
     * @param projectId 新增项目-产品需求id
     */
    void batchInsert(Long projectId,List<Long> productDemandIds);

    /**
     *
     * @param projectId 项目id
     * @return ProjectProductDemandDO
     */
    List<ProjectProductDemandDO> getByProjectId(Long projectId);

}
