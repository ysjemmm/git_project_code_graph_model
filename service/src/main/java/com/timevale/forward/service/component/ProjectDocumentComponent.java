package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectDocument;
import com.timevale.forward.dal.entity.ProjectNodeDO;

import java.util.List;

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

    /**
     *
     * @param projectId 项目id
     * @param projectNodeDOList 节点
     * @param type 项目类型
     * @return boolean
     */
    List<String> docNeedFillIn(Long projectId , List<ProjectNodeDO> projectNodeDOList, Integer type);

}
