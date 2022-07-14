package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectProductLineDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectProductLineMapper {

    /**
     *
     * @param projectId 项目id
     * @return 列表
     */
    List<ProjectProductLineDO> get(@Param("projectId") Long projectId);

    /**
     * 得到通过项目id列表
     *
     * @param projectIdList 项目id列表
     * @return 列表
     */
    List<ProjectProductLineDO> getByProjectIdList(@Param("projectIdList") List<Long> projectIdList);

    /**
     * 新增项目产品线
     *
     * @param projectProductLineDO 项目产品线
     * @return int
     */
    int batchInsert(List<ProjectProductLineDO> projectProductLineDO);

    /**
     * 更新项目产品线
     * @param projectProductLineDO 项目产品线
     * @return int
     */
    int update(ProjectProductLineDO projectProductLineDO);
    
    
}
