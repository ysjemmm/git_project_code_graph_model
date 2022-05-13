package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectPublishPlanDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectPublishPlanMapper {

    /**
     *
     * @param projectId 项目id
     * @return 列表
     */
    List<ProjectPublishPlanDO> get(@Param("projectId") Long projectId);
    /**
     * 新增项目发布计划
     *
     * @param projectPublishPlanDos 项目发布计划
     * @return int
     */
    int batchInsert(List<ProjectPublishPlanDO> projectPublishPlanDos);

    /**
     * 更新项目发布计划
     * @param projectPublishPlanDO 项目发布计划
     * @return int
     */
    int update(ProjectPublishPlanDO projectPublishPlanDO);
    
    
}
