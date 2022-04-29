package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectRiskExplanationMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.dal.entity.ProjectRiskExplanationDO;
import com.timevale.forward.service.component.ProjectRiskExplanationComponent;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
@LogPoint
public class ProjectRiskExplanationComponentImpl implements ProjectRiskExplanationComponent {

    @Resource
    private ProjectRiskMapper projectRiskMapper;

    @Resource
    private ProjectRiskExplanationMapper projectRiskExplanationMapper;

    @Override
    public void add(Long projectRiskId, String explanation) {
        ProjectRiskExplanationDO explanationDO = new ProjectRiskExplanationDO();
        explanationDO.setProjectRiskId(projectRiskId);
        explanationDO.setExplanation(explanation);

        // 插入后取最新值
        projectRiskExplanationMapper.insert(explanationDO);
        explanationDO = projectRiskExplanationMapper.selectById(explanationDO.getId());

        // 更新项目风险修改时间
        ProjectRiskDO riskDO = new ProjectRiskDO();
        riskDO.setId(projectRiskId);
        riskDO.setModifyDate(explanationDO.getModifyDate());
        projectRiskMapper.update(riskDO);
    }

    @Override
    public void batchAdd(List<ProjectRiskExplanationDO> explanationDOList) {
        if(CollectionUtils.isEmpty(explanationDOList)){
            return;
        }
        // 批量新增
        projectRiskExplanationMapper.batchInsert(explanationDOList);

        // 插入后取最新值
        Optional<ProjectRiskExplanationDO> any = explanationDOList.stream().findAny();
        ProjectRiskExplanationDO explanationDO = projectRiskExplanationMapper.selectById(any.get().getId());

        // 批量更新项目风险修改时间
        Date modifyDate = explanationDO.getModifyDate();
        List<Long> projectRiskIdList = explanationDOList.stream().map(ProjectRiskExplanationDO::getProjectRiskId).distinct().collect(Collectors.toList());
        projectRiskMapper.updateModifyDate(projectRiskIdList, modifyDate);
    }
}
