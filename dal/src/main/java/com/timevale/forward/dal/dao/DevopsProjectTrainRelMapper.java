package com.timevale.forward.dal.dao;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DevopsProjectTrainRelMapper {

    /**
     * 新增关联关系
     */
    @Insert("INSERT INTO devops_project_train_rel (project_id, publish_train_id, create_man, create_man_id, modify_man, modify_man_id, is_deleted) " +
            "VALUES (#{projectId}, #{publishTrainId}, #{createMan}, #{createManId}, #{createMan}, #{createManId}, false)")
    void insertRelation(@Param("projectId") Integer projectId,
                        @Param("publishTrainId") Integer publishTrainId,
                        @Param("createMan") String createMan,
                        @Param("createManId") String createManId);

    /**
     * 软删除关联关系
     */
    @Update("UPDATE devops_project_train_rel SET is_deleted = true, modify_man=#{modifyMan}, modify_man_id=#{modifyManId}, modify_date=NOW() " +
            "WHERE project_id = #{projectId} AND publish_train_id = #{publishTrainId} AND is_deleted = false")
    void deleteRelation(@Param("projectId") Integer projectId,
                        @Param("publishTrainId") Integer publishTrainId,
                        @Param("modifyMan") String modifyMan,
                        @Param("modifyManId") String modifyManId);

    /**
     * 检查关联关系是否存在
     */
    @Select("SELECT COUNT(1) FROM devops_project_train_rel WHERE project_id = #{projectId} AND publish_train_id = #{publishTrainId} AND is_deleted = false")
    int countRelation(@Param("projectId") Integer projectId, @Param("publishTrainId") Integer publishTrainId);

    /**
     * 检查关联关系是否存在1
     */
    @Select("SELECT COUNT(1) FROM devops_project_train_rel WHERE id = #{id} AND is_deleted = false")
    int countRelation1(@Param("id") Integer id);

    /**
     *
     * @param projectId 产研项目id
     * @return BaseResult<Integer>
     */
    @Select("SELECT publish_train_id FROM devops_project_train_rel " +
            "WHERE project_id = #{projectId} AND is_deleted = false " +
            "ORDER BY create_date DESC")
    List<Integer> selectTrainIdsByProjectId(@Param("projectId") Integer projectId);
}
