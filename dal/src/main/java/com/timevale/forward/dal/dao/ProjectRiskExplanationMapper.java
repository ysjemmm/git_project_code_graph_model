package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectRiskExplanationDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/04/24 15:50
 */
public interface ProjectRiskExplanationMapper {
    /**
     * 新增
     *
     * @param projectRiskExplanationDO 项目风险说明DO
     */
    int insert(ProjectRiskExplanationDO projectRiskExplanationDO);

    /**
     * 查询 by id
     *
     * @param id 项目风险说明 id
     */
    ProjectRiskExplanationDO selectById(@Param("id") Long id);

    /**
     * 查询 by 项目风险id
     *
     * @param projectRiskId 项目风险id
     */
    List<ProjectRiskExplanationDO> selectByProjectRiskId(@Param("projectRiskId") Long projectRiskId);

}
