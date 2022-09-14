package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectDocumentMapper;
import com.timevale.forward.dal.entity.ProjectDocument;
import com.timevale.forward.service.component.ProjectDocumentComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author xiaoyun
 * @date 2022/8/31/031 18:21
 */
@Component
@Slf4j
public class ProjectDocumentComponentImpl implements ProjectDocumentComponent {
    @Resource
    private ProjectDocumentMapper projectDocumentMapper;

    @Override
    public ProjectDocument getByProjectId(Long projectId, Integer type) {
        return projectDocumentMapper.getByProjectId(projectId, type);
    }

    @Override
    public Long insert(ProjectDocument projectDocument) {
        return projectDocumentMapper.insert(projectDocument);
    }

    @Override
    public void updateSelective(ProjectDocument projectDocument) {
        projectDocumentMapper.updateByPrimaryKeySelective(projectDocument);
    }
}
