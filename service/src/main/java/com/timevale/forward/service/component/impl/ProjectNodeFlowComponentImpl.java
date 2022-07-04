package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectNodeFlowMapper;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProjectNodeFlowComponentImpl implements ProjectNodeFlowComponent {

    @Resource
    private ProjectNodeFlowMapper projectNodeFlowMapper;

    @Override
    public void add(ProjectNodeFlowDO projectNodeFlowDO) {
        projectNodeFlowMapper.insert(projectNodeFlowDO);
    }
}
