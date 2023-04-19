package com.timevale.forward.dal.dao;

import generator.domain.ProjectBizDemandDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
* @author jingchun
* @description 针对表【project_biz_demand(项目-业务需求关系表(1-N))】的数据库操作Mapper
* @createDate 2023-04-17 14:17:41
* @Entity generator.domain.ProjectBizDemandDO
*/
public interface ProjectBizDemandMapper {

    /**
     * 根据业务需求id列表查询关联关系列表
     * @param bizDemandIds 业务需求列表
     * @return 业务需求和项目关联关系列表
     */
    List<ProjectBizDemandDO> selectByBizDemandIds(@Param("bizDemandIds") Collection<Long> bizDemandIds);

    @Select("select * from project_biz_demand where project_id = #{projectId} and is_deleted = false")
    List<ProjectBizDemandDO> selectByProjectId(@Param("projectId") Long projectId);

    void batchInsert(@Param("records") Collection<ProjectBizDemandDO> projectBizDemands);

    void delete(@Param("projectId") Long projectId, @Param("bizDemandIds")Collection<Long> bizDemandIds);

}




