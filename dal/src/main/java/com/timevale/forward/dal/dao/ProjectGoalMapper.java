package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectGoalDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
* @author jingchun
* description 针对表【project_goal(项目目标表)】的数据库操作Mapper
* created on 2022-06-20 14:48:55
*/
public interface ProjectGoalMapper {

    void unsetMainGoal(@Param("projectId") Long projectId);

    ProjectGoalDO getByName(@Param("projectId") Long projectId, @Param("name") String name);

    void insert(ProjectGoalDO projectGoal);

    ProjectGoalDO get(@Param("id") Long id);

    void update(ProjectGoalDO projectGoalDO);

    List<ProjectGoalDO> getByProjectId(@Param("projectId") Long id);

    void delete(@Param("id") Long projectGoalId);

    void batchInsert(Collection<ProjectGoalDO> convert);
}
