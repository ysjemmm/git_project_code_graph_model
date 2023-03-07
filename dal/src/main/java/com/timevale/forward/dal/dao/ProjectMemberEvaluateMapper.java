package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/06 18:30
 */
public interface ProjectMemberEvaluateMapper {

    void batchInsert(@Param("coll") Collection<ProjectMemberEvaluateDO> collection);

    void update(ProjectMemberEvaluateDO memberEvaluateDO);

    List<ProjectMemberEvaluateDO> selectByProjectId(@Param("projectId")Long projectId);

}