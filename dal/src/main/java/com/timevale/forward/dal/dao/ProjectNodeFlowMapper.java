package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface ProjectNodeFlowMapper {


    /**
     * 新增
     *
     * @param projectNodeFlowDO projectNodeFlowDO
     * @return int
     */
    int insert(ProjectNodeFlowDO projectNodeFlowDO);


    /**
     * 查询
     * @param id id
     * @return 项目流程信息
     */
    ProjectNodeFlowDO get(@Param("id") Long id, @Param("flowId") String flowId);


    /**
     * 更新单条项目
     *
     * @param projectNodeFlowDO 项目流程
     * @return int
     */
    int update(ProjectNodeFlowDO projectNodeFlowDO);

    /**
     * 查询
     * @param projectId projectId
     * @return 项目流程信息
     */
    List<ProjectNodeFlowDO> getByProjectId(@Param("projectId") Long projectId);


    List<ProjectNodeFlowDO> pageCompleteFlow();

    void batchUpdateFlowEndTime(List<ProjectNodeFlowDO> projectNodeFlowDOList);
}
