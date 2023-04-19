package com.timevale.forward.service.component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.model.enums.ProjectNodeStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
public class ProjectNodeComponent {

    @Resource
    private ProjectNodeMapper projectNodeMapper;


    /**
     * @param list      节点新增
     * @param projectId 项目id
     */
    public void add(List<ProjectNodeDO> list, Long projectId) {
        log.info("节点新增接收参数:list={},projectId={}", list, projectId);
        list.forEach(t -> t.setProjectId(projectId));
        projectNodeMapper.delete(projectId);
        projectNodeMapper.batchInsert(list);
    }


    /**
     * 节点信息
     *
     * @param projectId 项目id
     */
    public List<ProjectNodeDO> get(Long projectId) {
        return projectNodeMapper.get(projectId);
    }

    /**
     * 批量获取节点信息
     *
     * @param projectIdList 项目id
     * @return ProjectNodeDO
     */
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

    public void buildNodeForCustomerDevProject(Date projectStartDate, Date projectEndDate, Long projectId) {
        List<ProjectNodeDO> nodeList = ImmutableList.of(ProjectNodeEnum.START_PLAN, ProjectNodeEnum.DEMAND_CONSTRUE,
                ProjectNodeEnum.DEVELOP_START, ProjectNodeEnum.SUBMIT_TEST, ProjectNodeEnum.TEST_START,
                ProjectNodeEnum.PUBLISH_OFFICIAL).stream().map(n -> {
            ProjectNodeDO node = new ProjectNodeDO();
            node.setName(n.getText());
            if (n == ProjectNodeEnum.START_PLAN) {
                node.setPlanDate(projectStartDate);
                node.setActualDate(projectStartDate);
            } else if (n == ProjectNodeEnum.PUBLISH_OFFICIAL) {
                node.setPlanDate(projectEndDate);
            }
            return node;
        }).collect(Collectors.toList());
        add(nodeList, projectId);
    }

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

    /**
     * @param list      节点新增
     * @param projectId 项目id
     */
    public void updateNodeActualDate(List<ProjectNodeDO> list, Long projectId) {
        Map<String, ProjectNodeDO> newNodeMap = list.stream().collect(Collectors.toMap(ProjectNodeDO::getName, a -> a, (v1, v2) -> v2));
        //更新节点的实际时间,计划时间和老数据一致
        List<ProjectNodeDO> oldProjectNodes = projectNodeMapper.get(projectId);
        log.info("新节点:{},旧节点:{}", list, oldProjectNodes);
        oldProjectNodes.forEach(a -> {
            if (newNodeMap.containsKey(a.getName())) {
                a.setActualDate(newNodeMap.get(a.getName()).getActualDate());
            }
        });
        add(oldProjectNodes, projectId);
    }

    /**
     * @param list      节点新增
     * @param projectId 项目id
     */
    public void updateNodePlanDate(List<ProjectNodeDO> list, Long projectId) {
        //更新节点的计划时间,实际时间和老数据一致
        List<ProjectNodeDO> oldProjectNodes = projectNodeMapper.get(projectId);
        log.info("新节点:{},旧节点:{}", list, oldProjectNodes);
        Map<String, ProjectNodeDO> oldNodeMap = oldProjectNodes.stream().collect(Collectors.toMap(ProjectNodeDO::getName, a -> a, (v1, v2) -> v2));
        list.forEach(a -> {
            if (oldNodeMap.containsKey(a.getName())) {
                a.setActualDate(oldNodeMap.get(a.getName()).getActualDate());
            }
        });
        add(list, projectId);
    }

    /**
     * 得到实际日期为null的计划日期
     *
     * @param nodeDOList 节点DO列表
     * @return {@link Date}
     */
    public Date getRecentPlanDate(List<ProjectNodeDO> nodeDOList) {
        if (CollectionUtils.isEmpty(nodeDOList)) {
            return null;
        }
        nodeDOList = sort(nodeDOList);
        for (ProjectNodeDO e : nodeDOList) {
            if (e.getActualDate() == null) {
                return e.getPlanDate();
            }
        }
        return null;
    }

    /**
     * 节点排序
     *
     * @param nodeDOList 节点DO列表
     */
    public List<ProjectNodeDO> sort(List<ProjectNodeDO> nodeDOList) {
        Map<String, Integer> nodeMap = Arrays.stream(ProjectNodeEnum.values())
                .collect(Collectors.toMap(ProjectNodeEnum::getText, ProjectNodeEnum::getCode, (a, b) -> a));
        return nodeDOList.stream().sorted((a, b) -> {
            Integer aCode = nodeMap.get(a.getName());
            Integer bCode = nodeMap.get(b.getName());
            return aCode.compareTo(bCode);
        }).collect(Collectors.toList());
    }

    /**
     * 获得项目节点状态
     *
     * @param nodeDOList 节点DO列表
     * @return {@link Integer}
     */
    public Integer getStatus(List<ProjectNodeDO> nodeDOList) {
        nodeDOList = sort(nodeDOList);
        for (ProjectNodeDO e : nodeDOList) {
            if (e.getActualDate() == null) {
                return ProjectNodeStatusEnum.nodeStatusMap.get(e.getName());
            }
        }
        return ProjectNodeStatusEnum.PUBLISHED.getCode();
    }
}
