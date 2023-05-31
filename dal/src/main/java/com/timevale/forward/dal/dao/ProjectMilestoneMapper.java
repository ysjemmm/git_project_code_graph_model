package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectMilestone;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
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

    List<ProjectMilestone> selectByProjectIds(@Param("projectIds") List<Long> projectIds);

    void insert(ProjectMilestone entity);

    @Select("select * from project_milestone where id = #{id} and is_deleted = false")
    ProjectMilestone selectById(@Param("id") Long id);

    @Select("select * from project_milestone where relation_id = #{relationId} and type = #{type} and is_deleted = false limit 1")
    ProjectMilestone selectByRelation(@Param("relationId") Long relationId, @Param("type") Integer type);

    List<ProjectMilestone> selectByIds(@Param("list") List<Long> list);

    @Update("update project_milestone set is_deleted = true where id = #{id} and is_deleted = false")
    void deleteById(@Param("id") Long id);

    void update(ProjectMilestone milestone);

    @Select("SELECT * FROM project_milestone WHERE is_deleted = false")
    List<ProjectMilestone> selectAll();

    @Update("UPDATE info_forward.project_milestone SET plan_start_date = #{planStartDate}, plan_end_date=#{planEndDate}, modify_date = modify_date WHERE id=#{id}")
    void updateDate(@Param("id")Long id, @Param("planStartDate") Date planStartDate, @Param("planEndDate")Date planEndDate);
}




