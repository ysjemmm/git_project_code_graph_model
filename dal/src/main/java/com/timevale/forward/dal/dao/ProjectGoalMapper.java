package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectGoalDO;
import org.apache.ibatis.annotations.Param;

/**
* @author jingchun
* description 针对表【project_goal(项目目标表)】的数据库操作Mapper
* created on 2022-06-20 14:48:55
*/
public interface ProjectGoalMapper {

    void unsetMainGoal(@Param("projectId") Long projectId);

    ProjectGoalDO getByName(@Param("name") String name);

    void insert(ProjectGoalDO projectGoal);
}




