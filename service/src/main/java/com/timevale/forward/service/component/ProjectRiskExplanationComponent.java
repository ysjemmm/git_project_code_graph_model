package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectRiskExplanationDO;

import java.util.List;

public interface ProjectRiskExplanationComponent {

    /**
     * 添加
     *
     * @param projectRiskId 项目风险id
     * @param explanation   说明
     */
    void add(Long projectRiskId, String explanation);

    /**
     * 批量添加
     *
     * @param explanationDOList 项目风险说明dolist
     */
    void batchAdd(List<ProjectRiskExplanationDO> explanationDOList);
}
