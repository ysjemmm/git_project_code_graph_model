package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProjectNodeCondition;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * @author wangxuan
 */
public interface ProjectNodeMapper {
    /**
     * 新增单条项目
     *
     * @param projectNodeDO 项目
     * @return int
     */
    int batchInsert(List<ProjectNodeDO> projectNodeDO);

    /**
     * 删除
     *
     * @param projectId 项目id
     */
    void delete(@Param("projectId") Long projectId);

    /**
     * 查询
     *
     * @param projectId 项目id
     * @return int
     */
    List<ProjectNodeDO> get(@Param("projectId") Long projectId);


    /**
     * 查询 by 项目id列表
     *
     * @param projectIdList 项目id列表
     * @return {@code List<ProjectNodeDO>}
     */
    List<ProjectNodeDO> selectByProjectIdList(@Param("projectIdList") List<Long> projectIdList);

    /**
     * 查询 by 项目id-名字
     *
     * @param projectId 项目id
     * @param name      名字
     */
    ProjectNodeDO getByName(@Param("projectId") Long projectId, @Param("name") String name);


    /**
     * 更新实际提测时间
     *
     * @param projectId  项目id
     * @param actualDate 实际提测时间
     */
    void updateSubmitTestActualDate(@Param("projectId") Long projectId, @Param("actualDate") Date actualDate);

    /**
     * 更新实际时间
     *
     * @param id  id
     * @param actualDate 实际时间
     */
    void updateActualDateById(@Param("id") Long id, @Param("actualDate") Date actualDate);

    /**
     * 更新实际时间
     *
     * @param projectId  projectId
     * @param actualDate 实际时间
     */
    void updateEndDateByProjectIdAndName(@Param("projectId") Long projectId, @Param("name") String name, @Param("planDate") Date planDate, @Param("actualDate") Date actualDate, @Param("actualEndDate") Date actualEndDate);

    /**
     * 更新计划时间
     *
     * @param id  id
     * @param planDate 实际时间
     */
    @Update("UPDATE project_node SET plan_date = #{planDate} WHERE id = #{id}")
    void updatePlanDateById(@Param("id") Long id, @Param("planDate") Date planDate);

    /**
     *
     * @param condition condition
     * @return List
     */
    List<ProjectNodeDO> selectByCondition(@Param("c")ProjectNodeCondition condition);

    /**
     * 查询 by 项目id列表
     *
     * @param projectIdList 项目id列表
     * @return {@code List<ProjectNodeDO>}
     */
    List<ProjectNodeDO> selectByProjectIdListFilterDate(@Param("projectIdList") List<Long> projectIdList);

    /**
     * 查询
     *
     * @param id id
     * @return int
     */
    ProjectNodeDO getById(@Param("id") Long id);

}
