package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProjectNodeRecordDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:41
 */
public interface ProjectNodeRecordMapper {



    /**
     * 新增
     *
     * @param projectNodeRecordDO projectNodeRecordDO
     * @return int
     */
    int insert(ProjectNodeRecordDO projectNodeRecordDO);
    /**
     *
     * @param projectId projectId
     * @return ProjectNodeRecordDO
     */
    List<ProjectNodeRecordDO> list(@Param("projectId") Long projectId);


}
