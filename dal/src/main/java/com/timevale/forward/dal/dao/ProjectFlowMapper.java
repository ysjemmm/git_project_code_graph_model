package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectFlowDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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
     *
     * @param id id
     * @return 项目流程信息
     */
    ProjectFlowDO get(@Param("id") Long id, @Param("flowId") String flowId);

    /**
     * 查询
     *
     * @param projectId projectId
     * @return 项目流程信息
     */
    List<ProjectFlowDO> getByProjectId(@Param("projectId") Long projectId);

    @Select("SELECT * FROM project_flow WHERE flow_id = #{flowId} AND is_deleted = false")
    ProjectFlowDO getByFlowId(@Param("flowId") String flowId);

    /**
     * 新增单条项目
     *
     * @param projectFlowDO 项目流程
     * @return int
     */
    int update(ProjectFlowDO projectFlowDO);

    @Update("UPDATE project_flow SET `status`=#{status} WHERE id=#{id} ")
    void updateStatus(@Param("id")Long id, @Param("status")Integer status);

    /**
     * 根据项目id和流程类型获取
     *
     * @param projectId 项目id
     * @param flowType  流程类型
     * @return 流程列表
     */
    List<ProjectFlowDO> getByProjectIdAndType(@Param("projectId") Long projectId,
                                              @Param("flowType") Integer flowType);

    /**
     * 查询所有已完结的流程
     *
     * @return
     */
    List<ProjectFlowDO> pageCompleteFlow();

    /**
     * 批量更新
     *
     * @param projectFlowDOList
     */
    void batchUpdateFlowEndTime(@Param("projectFlowDOList") List<ProjectFlowDO> projectFlowDOList);

}
