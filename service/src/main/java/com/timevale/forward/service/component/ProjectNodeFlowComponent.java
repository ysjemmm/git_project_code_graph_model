package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.mandarin.common.query.QueryBase;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
public interface ProjectNodeFlowComponent {


    /**
     *
     * @param projectNodeFlowDO projectNodeFlowDO
     */
    void process(ProjectNodeFlowDO projectNodeFlowDO, List<ProjectNodeDO> projectNodes);

    /**
     *
     * @param processInstanceId processInstanceId
     */
    void  updateProjectNodeInfo(String processInstanceId);

    /**
     * 新增项目节点记录
     *
     * @param projectId    项目id
     * @param projectNodes 项目节点
     */
    void  insertProjectNodeRecord(Long projectId,List<ProjectNodeDO> projectNodes);

    List<ProjectNodeFlowDO> flushCompleteFlow(QueryBase queryBase);
}
