package com.timevale.forward.service.component;

import com.timevale.forward.facade.api.request.ProjectNodeAddReq;

import java.util.List;

public interface ProjectNodeComponent {
    /**
     * 
     * @param list 节点新增
     * @param projectId 项目id
     */
    void add(List<ProjectNodeAddReq> list,Long projectId);

}
