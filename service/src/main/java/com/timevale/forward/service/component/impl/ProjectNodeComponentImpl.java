package com.timevale.forward.service.component.impl;

import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.model.enums.ProjectNodeStatusEnum;
import com.timevale.forward.service.component.ProjectNodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
    public Map<Long, List<ProjectNodeDO>> get(List<Long> projectIdList) {
        log.info("节点查询接收参数:projectIdList={}", projectIdList);

        Map<Long, List<ProjectNodeDO>> result = Maps.newHashMap();

        // 入参判空
        if (CollectionUtils.isEmpty(projectIdList)) {
            return result;
        }

        // 查询数据
        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.selectByProjectIdList(projectIdList);
        result.putAll(projectNodeDOList.stream().collect(Collectors.groupingBy(ProjectNodeDO::getProjectId)));

        return result;
    }

    @Override
    public void buildDefaultNode(Date projectStartDate, Date projectEndDate, Long projectId) {
        List<ProjectNodeDO> nodeDOList = new ArrayList<>();

        // 遍历
        ProjectNodeEnum[] nodeEnums = ProjectNodeEnum.values();
        for (ProjectNodeEnum e : nodeEnums) {
            ProjectNodeDO nodeDO = new ProjectNodeDO();

            nodeDO.setName(e.getText());
            if (ProjectNodeEnum.START_PLAN.equals(e)) {
                nodeDO.setPlanDate(projectStartDate);
            }
            if (ProjectNodeEnum.PUBLISH_OFFICIAL.equals(e)) {
                nodeDO.setPlanDate(projectEndDate);
            }
            nodeDOList.add(nodeDO);
        }
        add(nodeDOList, projectId);
    }

    @Override
    public void updateNodeActualDate(List<ProjectNodeDO> list, Long projectId) {
        Map<String, ProjectNodeDO> newNodeMap = list.stream().collect(Collectors.toMap(ProjectNodeDO::getName, a -> a, (v1, v2) -> v2));
        //更新节点的实际时间,计划时间和老数据一致
        List<ProjectNodeDO> oldProjectNodes = projectNodeMapper.get(projectId);
        log.info("新节点:{},旧节点:{}",list,oldProjectNodes);
        oldProjectNodes.forEach(a -> {
            if (newNodeMap.containsKey(a.getName())) {
                a.setActualDate(newNodeMap.get(a.getName()).getActualDate());
            }
        });
        add(oldProjectNodes, projectId);
    }

    @Override
    public void updateNodePlanDate(List<ProjectNodeDO> list, Long projectId) {
        //更新节点的计划时间,实际时间和老数据一致
        List<ProjectNodeDO> oldProjectNodes = projectNodeMapper.get(projectId);
        log.info("新节点:{},旧节点:{}",list,oldProjectNodes);
        Map<String, ProjectNodeDO> oldNodeMap = oldProjectNodes.stream().collect(Collectors.toMap(ProjectNodeDO::getName, a -> a, (v1, v2) -> v2));
        list.forEach(a -> {
            if (oldNodeMap.containsKey(a.getName())) {
                a.setActualDate(oldNodeMap.get(a.getName()).getActualDate());
            }
        });
        add(list, projectId);
    }

    @Override
    public Date getRecentPlanDate(List<ProjectNodeDO> nodeDOList) {
        nodeDOList = sort(nodeDOList);
        for (ProjectNodeDO e : nodeDOList) {
            if(e.getActualDate() == null){
                return e.getPlanDate();
            }
        }
        return null;
    }

    @Override
    public List<ProjectNodeDO> sort(List<ProjectNodeDO> nodeDOList) {
        Map<String, Integer> nodeMap = Arrays.stream(ProjectNodeEnum.values())
                .collect(Collectors.toMap(ProjectNodeEnum::getText, ProjectNodeEnum::getCode, (a, b) -> a));
        return nodeDOList.stream().sorted((a, b) -> {
            Integer aCode = nodeMap.get(a.getName());
            Integer bCode = nodeMap.get(b.getName());
            return aCode.compareTo(bCode);
        }).collect(Collectors.toList());
    }

    @Override
    public Integer getStatus(List<ProjectNodeDO> nodeDOList) {
        nodeDOList = sort(nodeDOList);
        for (ProjectNodeDO e : nodeDOList) {
            if(e.getActualDate() == null){
                return ProjectNodeStatusEnum.nodeStatusMap.get(e.getName());
            }
        }
        return ProjectNodeStatusEnum.PUBLISHED.getCode();
    }

    private void fillValue(Long projectId, List<ProjectNodeDO> projectNodeDO) {
        projectNodeDO.forEach(t -> {
            t.setProjectId(projectId);
        });
    }
}
