package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectMilestoneActionDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
* @author yangxu
* @date 2023-05-25 15:36:24
*/
public interface ProjectMilestoneActionMapper {

    void add(ProjectMilestoneActionDO actionDO);

    void batchAdd(@Param("actions")Collection<ProjectMilestoneActionDO> actions);

    void del(@Param("id")Long id);

    void delByMain(@Param("milestoneId")Long milestoneId);

    @Select("SELECT * FROM project_milestone_action WHERE relation_id=#{relationId} AND type=#{type} AND is_deleted=false")
    ProjectMilestoneActionDO getOne(@Param("relationId")Long relationId, @Param("type")Integer type);

    List<ProjectMilestoneActionDO> getByType(@Param("milestoneId")Long milestoneId, @Param("type")Integer type);

    List<ProjectMilestoneActionDO> getByMain(@Param("milestoneId")Long milestoneId);
}




