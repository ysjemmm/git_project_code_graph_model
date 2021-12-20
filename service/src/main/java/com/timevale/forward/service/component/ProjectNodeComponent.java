package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;

import java.util.List;

public interface ProjectNodeComponent {
    /**
     * 
     * @param list 节点新增
     * @param projectId 项目id
     */
    void add(List<ProjectNodeAddReq> list,Long projectId);

    /**
     * 节点信息
     * @param projectId 项目id
     * @return ProjectNodeDO
     */
    List<ProjectNodeDO> get(Long projectId);

}
