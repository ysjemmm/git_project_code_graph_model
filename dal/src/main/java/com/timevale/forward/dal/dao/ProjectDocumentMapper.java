package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectDocument;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:11
 */
public interface ProjectDocumentMapper {
    Long insert(ProjectDocument record);

    int insertSelective(ProjectDocument record);

    ProjectDocument selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ProjectDocument record);

    int updateByPrimaryKey(ProjectDocument record);

    int batchInsert(@Param("list") List<ProjectDocument> list);

    ProjectDocument getByProjectId(@Param("projectId") Long projectId, @Param("type") Integer type);

    @Select("select * from project_document where project_id = #{projectId} and is_deleted = false")
    List<ProjectDocument> listByProjectId(@Param("projectId") Long projectId);

    void delete(@Param("id") Long id);
}