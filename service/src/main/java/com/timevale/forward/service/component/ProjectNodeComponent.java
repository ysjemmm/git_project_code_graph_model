package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProjectNodeDO;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ProjectNodeComponent {
    /**
     * 
     * @param list 节点新增
     * @param projectId 项目id
     */
    void add(List<ProjectNodeDO> list,Long projectId);

    /**
     * 节点信息
     * @param projectId 项目id
     * @return ProjectNodeDO
     */
    List<ProjectNodeDO> get(Long projectId);

    /**
     * 批量获取节点信息
     * @param projectIdList 项目id
     * @return ProjectNodeDO
     */
    Map<Long,List<ProjectNodeDO>> get(List<Long> projectIdList);

    /**
     *
     * @param projectStartDate projectStartDate
     * @param projectEndDate  projectEndDate
     * @param projectId  projectId
     */
    void buildDefaultNode(Date projectStartDate,Date projectEndDate, Long projectId);

    /**
     *
     * @param list 节点新增
     * @param projectId 项目id
     */
    void updateNodeActualDate(List<ProjectNodeDO> list,Long projectId);

    /**
     *
     * @param list 节点新增
     * @param projectId 项目id
     */
    void updateNodePlanDate(List<ProjectNodeDO> list,Long projectId);

    /**
     * 得到实际日期为null的计划日期
     *
     * @param nodeDOList 节点DO列表
     * @return {@link Date}
     */
    Date getRecentPlanDate(List<ProjectNodeDO> nodeDOList);

    /**
     * 节点排序
     *
     * @param nodeDOList 节点DO列表
     */
    List<ProjectNodeDO> sort(List<ProjectNodeDO> nodeDOList);

    /**
     * 获得项目节点状态
     *
     * @param nodeDOList 节点DO列表
     * @return {@link Integer}
     */
    Integer getStatus(List<ProjectNodeDO> nodeDOList);

}
