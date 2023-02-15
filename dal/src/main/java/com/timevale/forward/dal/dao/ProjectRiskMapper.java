package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProjectRiskCondition;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    void updateStatusByMainId(@Param("mainId") Long mainId, @Param("status") Integer status, @Param("types")List<Integer> types);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 批量更新状态
     *
     * @param idList id列表
     * @param status  状态
     */
    int updateStatuses(@Param("idList") List<Long> idList, @Param("status") Integer status);

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
    List<ProjectRiskDO> selectByProjectIdListStatus(@Param("projectIdList") List<Long>projectIdList, @Param("status") Integer status);

    /**
     * 查询 by 状态, 不包含类型：其它
     *
     * @param status 项目id
     */
    List<ProjectRiskDO> selectByStatus(@Param("status") Integer status);

    /**
     *
     * @param status status
     * @param type type
     * @return 列表
     */
    List<ProjectRiskDO> selectByStatusType(@Param("status") Integer status,@Param("type") Integer type);


    /**
     * 选择 by 状态类型
     *
     * @param status status
     * @param types  类型
     * @return 列表
     */
    List<ProjectRiskDO> selectByStatusTypes(@Param("status") Integer status,@Param("types") List<Integer> types);

    /**
     * 统计个数
     *
     * @param projectId 项目id
     * @param status    状态
     * @return {@link Long}
     */
    Long count(@Param("projectId") Long projectId, @Param("status") Integer status);

    @Select("SELECT * FROM info_forward.project_risk WHERE project_id = #{projectId} AND name = #{name} AND is_deleted = FALSE")
    List<ProjectRiskDO> selectByName(@Param("projectId") Long projectId, @Param("name") String name);

    List<ProjectRiskDO> selectByMain(@Param("mainId") Long mainId, @Param("status") Integer status, @Param("types") List<Integer> types);
}
