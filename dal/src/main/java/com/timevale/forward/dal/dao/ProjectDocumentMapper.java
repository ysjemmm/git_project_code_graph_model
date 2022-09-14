package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectDocument;

import java.util.List;

import org.apache.ibatis.annotations.Param;

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
}