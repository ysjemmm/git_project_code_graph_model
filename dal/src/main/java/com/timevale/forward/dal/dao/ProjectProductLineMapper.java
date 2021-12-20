package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectProductLineDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectProductLineMapper {

    /**
     * 
     * @param projectI 项目id
     * @return 列表
     */
    List<ProjectProductLineDO> get(@Param("projectId") Long projectI);
    /**
     * 新增项目
     *
     * @param projectProductLineDO 项目
     * @return int
     */
    int batchInsert(List<ProjectProductLineDO> projectProductLineDO);


    int update(ProjectProductLineDO projectProductLineDO);
    
    
}
