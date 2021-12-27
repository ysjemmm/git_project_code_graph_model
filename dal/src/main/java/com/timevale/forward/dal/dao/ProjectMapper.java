package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProjectMapper {
    /**
     * 新增单条项目
     *
     * @param projectDO 项目
     * @return int
     */
    int insert(ProjectDO projectDO);

    /**
     * 查询
     * @param id id
     * @return 项目信息
     */
    ProjectDO get(@Param("id") Long id);


    /**
     * 新增单条项目
     *
     * @param projectDO 项目
     * @return int
     */
    int update(ProjectDO projectDO);

    /**
     * 新增单条项目
     *
     * @param condition 项目
     * @return int
     */
    List<ProjectListDO> list(ProjectListCondition condition);

    /**
     * 查数量
     *
     * @param condition 项目
     * @return int
     */
    int count(ProjectListCondition condition);


    /**
     * 查询
     * @param productDemandId 产品需求id
     * @return 项目信息
     */
    ProjectDO getByProductDemandId(@Param("productDemandId") Long productDemandId);


}
