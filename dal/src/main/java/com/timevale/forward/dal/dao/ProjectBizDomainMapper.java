package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectBizDomainDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
* @author yangxu
* @date 2023-05-24 16:30:51
*/
public interface ProjectBizDomainMapper {

    @Select("SELECT * FROM project_biz_domain WHERE project_id =#{projectId} AND is_deleted=false")
    List<ProjectBizDomainDO> getByProjectId(@Param("projectId") Long projectId);

    List<ProjectBizDomainDO> getByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    void batchAdd(@Param("projectId") Long projectId, @Param("bizDomainIds") Collection<Long> bizDomainIds);

    void batchDel(@Param("projectId") Long projectId, @Param("bizDomainIds") Collection<Long> bizDomainIds);

    List<Long> in(@Param("projectIds") Collection<Long> projectIds, @Param("bizDomainIds") Collection<Long> bizDomainIds);
}




