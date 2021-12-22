package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectProductDemandDO;

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


}
