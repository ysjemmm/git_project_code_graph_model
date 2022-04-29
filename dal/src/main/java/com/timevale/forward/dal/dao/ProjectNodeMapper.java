package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import org.apache.ibatis.annotations.Param;

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
     * 删除
     *
     * @param projectIds 项目id
     * @return ProjectNodeDO
     */
    List<ProjectNodeDO> getByProjectIds(@Param("projectIds") List<Long> projectIds);
}
