package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectDO;
import org.apache.ibatis.annotations.Param;

public interface ProjectMapper {
    /**
     * 新增单条项目
     *
     * @param projectDO 项目
     * @return int
     */
    int insert(ProjectDO projectDO);

    ProjectDO get(@Param("id") Long id);
    


}
