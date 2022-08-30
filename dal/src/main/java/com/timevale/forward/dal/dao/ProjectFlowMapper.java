package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectFlowDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectFlowMapper {
    /**
     * 新增单条项目
     *
     * @param projectFlowDO 项目流程
     * @return int
     */
    int insert(ProjectFlowDO projectFlowDO);

    /**
     * 查询
     * @param id id
     * @return 项目流程信息
     */
    ProjectFlowDO get(@Param("id") Long id,@Param("flowId") String flowId);

    /**
     * 查询
     * @param projectId projectId
     * @return 项目流程信息
     */
    List<ProjectFlowDO> getByProjectId(@Param("projectId") Long projectId);


    /**
     * 新增单条项目
     *
     * @param projectFlowDO 项目流程
     * @return int
     */
    int update(ProjectFlowDO projectFlowDO);

    /**
     * 根据项目id和流程类型获取
     * @param projectId 项目id
     * @param flowType 流程类型
     * @return 流程列表
     */
    List<ProjectFlowDO> getByProjectIdAndType(@Param("projectId") Long projectId,
                                              @Param("flowType") Integer flowType);
}
