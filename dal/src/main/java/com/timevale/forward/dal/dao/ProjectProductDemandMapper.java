package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
public interface ProjectProductDemandMapper {

    /**
     * 删除
     *
     * @param projectProductDemandDO 项目
     * @return int
     */
    int update(ProjectProductDemandDO projectProductDemandDO);

    /**
     * 新增项目-产品需求
     *
     * @param projectProductLineDO 新增项目-产品需求
     * @return int
     */
    int batchInsert(List<ProjectProductDemandDO> projectProductLineDO);


    /**
     *
     * @param projectId 项目id
     * @return ProjectProductDemandDO
     */
    List<ProjectProductDemandDO> getByProjectId(@Param("projectId") Long projectId);

    /**
     *
     * @param productDemandId 产品需求id
     * @return ProjectProductDemandDO
     */
    ProjectProductDemandDO getByProductDemandId(@Param("productDemandId") Long productDemandId);



    /**
     *
     * @return ProjectProductDemandDO
     */
    List<ProjectProductDemandDO> getLinkedProductDemand(@Param("productDemandIds") Collection<Long> productDemandIds);
}
