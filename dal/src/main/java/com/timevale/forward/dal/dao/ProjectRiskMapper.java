package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProjectRiskCondition;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
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
     * @param status  状态
     */
    int updateStatus(@Param("idList") List<Long> idList, @Param("status") Integer status);

    /**
     * 批量更新修改时间
     *
     * @param idList id列表
     * @param modifyDate  修改时间
     */
    int updateModifyDate(@Param("idList") List<Long> idList, @Param("modifyDate") Date modifyDate);

    /**
     * 查询 by id
     *
     * @param id 项目id
     */
    ProjectRiskDO selectById(@Param("id") Long id);

    /**
     * 查询 by 条件
     *
     * @param condition 条件
     * @return {@link List}<{@link ProjectRiskDO}>
     */
    List<ProjectRiskDO> select(ProjectRiskCondition condition);

    /**
     * 查询 by 项目id
     *
     * @param projectId 项目id
     */
    List<ProjectRiskDO> selectByProjectId(@Param("projectId") Long projectId);

    /**
     * 查询 by 项目id列表
     *
     * @param projectIdList 项目id列表
     */
    List<ProjectRiskDO> selectByProjectIdList(@Param("projectIdList") List<Long>projectIdList);

    /**
     * 查询 by 项目id列表 状态
     *
     * @param projectIdList 项目id列表
     */
    List<ProjectRiskDO> selectByProjectIdListStatus(@Param("projectIdList") List<Long>projectIdList, @Param("statusList") List<Integer> statusList);

    /**
     * 查询 by 状态, 不包含类型：其它
     *
     * @param status 项目id
     */
    List<ProjectRiskDO> selectByStatus(@Param("status") Integer status);

}
