package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectNodeRecordDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface ProjectNodeRecordMapper {
    /**
     *
     * @param projectNodeRecordDos projectNodeRecordDos
     * @return int
     */
    int batchInsert(List<ProjectNodeRecordDO> projectNodeRecordDos);
    /**
     *
     * @param projectId projectId
     * @return ProjectNodeRecordDO
     */
    List<ProjectNodeRecordDO> list(@Param("projectId") Long projectId);

    /**
     * 是否包含节点记录——基线版本
     *
     * @return boolean
     */
    @Select("SELECT COUNT(*) FROM project_node_record WHERE project_id = #{projectId} AND is_deleted = false")
    boolean contain(@Param("projectId")Long projectId);

}
