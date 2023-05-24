package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectPbuDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
* @author yangxu
* @date 2023-05-24 16:30:38
*/
public interface ProjectPbuMapper {

    void batchAdd(@Param("projectId") Long projectId, @Param("pbuIds") Collection<Long>pbuIds);

    void batchDel(@Param("projectId") Long projectId, @Param("pbuIds") Collection<Long>pbuIds);

    @Select("SELECT * FROM project_pbu WHERE project_id =#{projectId} AND is_deleted=false")
    List<ProjectPbuDO> getByProjectId(@Param("projectId")Long projectId);

    List<ProjectPbuDO> getByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    List<Long> in(@Param("projectIds") Collection<Long> projectIds, @Param("pbuIds") Collection<Long> pbuIds);
}




