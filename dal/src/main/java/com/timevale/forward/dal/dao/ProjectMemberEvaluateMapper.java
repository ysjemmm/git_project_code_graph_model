package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/06 18:30
 */
public interface ProjectMemberEvaluateMapper {

    void batchInsert(@Param("coll") Collection<ProjectMemberEvaluateDO> collection);

    void batchDelete(@Param("projectId")Long projectId, @Param("userIdColl") Collection<String> userIdColl);

    void update(ProjectMemberEvaluateDO memberEvaluateDO);

    void updatePlanWorkload(ProjectMemberEvaluateDO memberEvaluateDO);

    List<ProjectMemberEvaluateDO> getByProjectId(@Param("projectId")Long projectId);

    @Select("SELECT * FROM info_forward.project_member_evaluate WHERE project_id=#{projectId} AND user_id=#{userId} AND is_deleted=false LIMIT 1")
    ProjectMemberEvaluateDO getPerson(@Param("projectId")Long projectId, @Param("userId")String userId);
}