package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectNodeCopier;
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
    public void add(List<ProjectNodeAddReq> list,Long projectId) {
        log.info("节点新增接收参数:list={},projectId={}", list, projectId);
        List<ProjectNodeDO> projectNodeDO = ProjectNodeCopier.INSTANCE.convert(list);
        fillValue(projectId,projectNodeDO);
        projectNodeMapper.delete(projectId);
        projectNodeMapper.batchInsert(projectNodeDO);
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
