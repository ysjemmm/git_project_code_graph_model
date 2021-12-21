package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectProductDemandDO;

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

}
