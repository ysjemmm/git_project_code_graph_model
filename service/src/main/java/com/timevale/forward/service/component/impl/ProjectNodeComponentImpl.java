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
import java.util.*;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectNodeComponentImpl implements ProjectNodeComponent {

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    private final Map<Integer, String> defaultNode = new HashMap<Integer, String>() {{
        put(1, "开始规划");
        put(2, "需求内审");
        put(3, "需求串讲");
        put(4, "技术详设评审");
        put(5, "开发开始");
        put(6, "编写测试用例");
        put(7, "用例评审");
        put(8, "提测");
        put(9, "测试开始");
        put(10, "发布模拟");
        put(11, "发布正式");
    }};

    @Override
    public void add(List<ProjectNodeDO> list, Long projectId) {
        log.info("节点新增接收参数:list={},projectId={}", list, projectId);
        fillValue(projectId, list);
        projectNodeMapper.delete(projectId);
        projectNodeMapper.batchInsert(list);
    }

    @Override
    public List<ProjectNodeDO> get(Long projectId) {
        return projectNodeMapper.get(projectId);
    }

    @Override
    public void buildDefaultNode(Date projectStartDate, Date projectEndDate, Long projectId) {
        List<ProjectNodeDO> list = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            ProjectNodeDO nodeDO = new ProjectNodeDO();
            nodeDO.setName(defaultNode.get(i));
            if (i == 1) {
                nodeDO.setPlanDate(projectStartDate);
            }
            if (i == 11) {
                nodeDO.setPlanDate(projectEndDate);
            }
            list.add(nodeDO);
        }
        add(list, projectId);
    }

    private void fillValue(Long projectId, List<ProjectNodeDO> projectNodeDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        projectNodeDO.forEach(t -> {
            t.setProjectId(projectId);
            t.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            t.setCreateManId(userInfo.getId());
        });
    }
}
