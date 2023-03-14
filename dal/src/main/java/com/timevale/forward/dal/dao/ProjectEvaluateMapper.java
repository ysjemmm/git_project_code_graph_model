package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectEvaluateDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/06 18:30
 */
public interface ProjectEvaluateMapper {

    List<ProjectEvaluateDO> selectByProjectId(@Param("projectId") Long projectId);

    void update(ProjectEvaluateDO evaluateDO);

    @Update("UPDATE project_evaluate SET scores = #{scores} WHERE project_id = #{projectId} AND evaluate_dimension_id = #{evaluateDimensionId}")
    void updateScores(ProjectEvaluateDO evaluateDO);

    void batchInsert(@Param("projectId")Long projectId, @Param("dimensionIds")Collection<Long> dimensionIds);

}