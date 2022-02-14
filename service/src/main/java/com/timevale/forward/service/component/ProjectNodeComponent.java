package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectNodeDO;

import java.util.Date;
import java.util.List;

public interface ProjectNodeComponent {
    /**
     * 
     * @param list 节点新增
     * @param projectId 项目id
     */
    void add(List<ProjectNodeDO> list,Long projectId);

    /**
     * 节点信息
     * @param projectId 项目id
     * @return ProjectNodeDO
     */
    List<ProjectNodeDO> get(Long projectId);


    /**
     *
     * @param projectStartDate projectStartDate
     * @param projectEndDate  projectEndDate
     * @param projectId  projectId
     */
    void buildDefaultNode(Date projectStartDate,Date projectEndDate, Long projectId);

}
