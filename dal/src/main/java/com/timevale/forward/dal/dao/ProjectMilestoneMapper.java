package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectMilestone;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author jingchun
* @description 针对表【project_milestone(项目里程碑)】的数据库操作Mapper
* @createDate 2023-02-01 14:46:42
* @Entity com.timevale.forward.dal.entity.ProjectMilestone
*/
public interface ProjectMilestoneMapper {

    @Select("select * from project_milestone where project_id = #{projectId} and is_deleted = false")
    List<ProjectMilestone> selectByProjectId(@Param("projectId") Long projectId);

    void insert(ProjectMilestone entity);
}




