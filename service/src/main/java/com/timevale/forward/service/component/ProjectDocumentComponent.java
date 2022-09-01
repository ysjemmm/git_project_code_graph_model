package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectDocument;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:20
 */
public interface ProjectDocumentComponent {

    /**
     * 根据项目id获取
     * @param projectId
     * @param type
     * @return
     */
    ProjectDocument getByProjectId(Long projectId, Integer type);

    /**
     * 新增
     * @param projectDocument
     */
    Long insert(ProjectDocument projectDocument);

    /**
     * 更新Selective
     * @param projectDocument
     */
    void updateSelective(ProjectDocument projectDocument);
}
