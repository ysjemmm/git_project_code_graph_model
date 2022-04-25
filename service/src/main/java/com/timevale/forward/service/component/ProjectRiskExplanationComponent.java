package com.timevale.forward.service.component;

public interface ProjectRiskExplanationComponent {

    /**
     * 添加
     *
     * @param projectRiskId 项目风险id
     * @param explanation   说明
     */
    void add(Long projectRiskId, String explanation);
}
