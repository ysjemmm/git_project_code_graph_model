package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
     * @param projectId 项目id
     * @return int 
     */
    int delete(@Param("projectId") Long projectId);


}
