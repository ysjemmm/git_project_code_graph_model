package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectNodeComponentImpl implements ProjectNodeComponent {

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Override
    public void add(List<ProjectNodeDO> list,Long projectId) {
        log.info("节点新增接收参数:list={},projectId={}", list, projectId);
        fillValue(projectId,list);
        projectNodeMapper.delete(projectId);
        projectNodeMapper.batchInsert(list);
    }

    @Override
    public List<ProjectNodeDO> get(Long projectId) {
        return projectNodeMapper.get(projectId);
    }

    private void fillValue(Long projectId,List<ProjectNodeDO> projectNodeDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        projectNodeDO.forEach(t -> {
            t.setProjectId(projectId);
            t.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            t.setCreateManId(userInfo.getId());
        });
    }
}
