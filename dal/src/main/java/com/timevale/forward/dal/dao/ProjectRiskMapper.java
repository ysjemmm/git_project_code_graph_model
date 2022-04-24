package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectRiskDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/04/24 15:50
 */
public interface ProjectRiskMapper {
    /**
     * 新增
     *
     * @param projectRiskDO 项目风险DO
     */
    int insert(ProjectRiskDO projectRiskDO);

    /**
     * 批量新增
     *
     * @param projectRiskDOList 项目风险DO 列表
     */
    int batchInsert(@Param("projectRiskDOList") List<ProjectRiskDO> projectRiskDOList);

    /**
     * 更新
     *
     * @param projectRiskDO 项目风险DO
     */
    int update(ProjectRiskDO projectRiskDO);

    /**
     * 批量更新状态
     *
     * @param idList id列表
     * @param state  状态
     */
    int updateState(@Param("idList") List<Long> idList, @Param("state") Integer state);

    /**
     * 查询 by id
     *
     * @param id 项目id
     */
    ProjectRiskDO selectById(@Param("id") Long id);

    /**
     * 查询 by 项目id
     *
     * @param projectId 项目id
     */
    List<ProjectRiskDO> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 查询 by 状态
     *
     * @param state 项目id
     */
    List<ProjectRiskDO> selectByState(@Param("state") Integer state);

}
