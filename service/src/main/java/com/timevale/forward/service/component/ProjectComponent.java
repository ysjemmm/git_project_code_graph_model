package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;

import java.util.List;

public interface ProjectComponent {
    /**
     *
     * @param projectListCondition 查询条件
     * @return 列表
     */
    QueryResultVO<ProjectVO> page(ProjectListCondition projectListCondition, List<Long> projectIds);

    /**
     * 填充项目信息
     * @param projectNodes projectNodes
     * @param projectDO projectDO
     */
    void fillInfo(List<ProjectNodeDO> projectNodes, ProjectDO projectDO);

    /**
     * 根据项目节点，获取项目状态
     *
     * @param projectId 项目id
     */
    Integer getStatus(Long projectId);

    /**
     * 更新项目节点状态
     *
     * @param projectId 项目id
     */
    void updateNodeStatus(Long projectId);

    /**
     *
     * @param projectId projectId
     * @return Long
     */
    List<Long> getLinkBizDemandIds(Long projectId);
}
