package com.timevale.forward.service.job;

import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.ProjectGoalStatusEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import com.timevale.mandarin.base.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * create on 2022/6/27
 */
@Slf4j
@JobHandler(value = "ProjectGoalReachDateNotifyJob")
public class ProjectGoalReachDateNotifyJob extends IJobHandler {

    private static final String NOTIFY_PATTERN = "项目:%s目标达成时间已到期，请及时更新项目目标完成情况。";
    private static final String TITLE = "项目目标到期提醒";
    private static final String EXCLUDE_BIZ_DOMAIN = "数智化中心";

    @Value("${projectGoal.receiver:zhuque}")
    private String reachGoalReceiver;

    @Resource
    private ProjectGoalMapper projectGoalMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ErpMessageClient erpMessageClient;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProjectProductLineMapper projectProductLineMapper;

    @Override
    public ReturnT<String> execute(String s) {
        log.info("开始项目目标达成日期通知任务");
        List<ProjectGoalDO> projectGoals = projectGoalMapper.getByDate(new Date());
        Set<Long> projectIds = projectGoals.stream().map(ProjectGoalDO::getProjectId)
                .collect(Collectors.toSet());
        List<ProjectDO> projects = projectMapper.getByIds(projectIds);

        // 项目关联产品线
        List<ProjectProductLineDO> projectProductLineDOList = projectProductLineMapper.getByProjectIdList(new ArrayList<>(projectIds));
        Map<Long, Set<Long>> projectProductLineGroup = new HashMap<>();
        for (ProjectProductLineDO e : projectProductLineDOList) {
            Long projectId = e.getProjectId();
            Long productLineId = e.getProductLineId();

            Set<Long> productLineIdSet = projectProductLineGroup.get(projectId);
            if (CollectionUtils.isEmpty(productLineIdSet)) {
                productLineIdSet = new HashSet<>();
            }
            productLineIdSet.add(productLineId);
            projectProductLineGroup.put(projectId, productLineIdSet);
        }

        // 产品线、业务域信息
        List<Long> productLineIdList = projectProductLineDOList.stream().map(ProjectProductLineDO::getProductLineId).collect(Collectors.toList());
        List<ProductLineDO> productLineDOList = productLineMapper.selectByIds(productLineIdList);
        Set<Long> bizDomainIdSet = productLineDOList.stream().map(ProductLineDO::getBizDomainId).collect(Collectors.toSet());
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByIdList(new ArrayList<>(bizDomainIdSet));
        Map<Long, String> bizDomainNameMap = bizDomainDOList.stream().collect(Collectors.toMap(BaseDO::getId, BizDomainDO::getName));
        Map<Long, String> productLineBizDomainGroup = productLineDOList.stream().collect(Collectors.toMap(BaseDO::getId, e->bizDomainNameMap.get(e.getBizDomainId())));

        Map<Long, ProjectDO> projectById = Maps.uniqueIndex(projects, ProjectDO::getId);
        for (ProjectGoalDO projectGoal : projectGoals) {
            ProjectDO project = projectById.get(projectGoal.getProjectId());

            if (project == null || YesOrNoEnum.NO.getCode().equals(project.getIsWithGoal())) {
                // 无项目或者无项目目标，过滤
                log.info("项目无项目目标，过滤目标: {}", projectGoal);
                continue;
            }
            if (project.getStatus() < 0) {
                // 已作废、暂停项目，过滤
                log.info("项目已经暂停或者作废，过滤目标: {}", projectGoal);
                continue;
            }
            if (!ProjectGoalStatusEnum.IN_PROGRESS.getCode().equals(projectGoal.getStatus())) {
                // 非进行中项目目标不再提醒
                log.info("项目目标已完成，过滤目标: {}", projectGoal);
                continue;
            }
            log.info("通知项目到期: {}", projectGoal);

            // 如果业务域仅=数智化中，则不通知
            Long projectId = project.getId();
            Set<Long> productLineIdSet = projectProductLineGroup.get(projectId);
            Set<String> bizDomainNameSet = productLineIdSet.stream().map(productLineBizDomainGroup::get).collect(Collectors.toSet());
            if(bizDomainNameSet.size() == 1 && bizDomainNameSet.contains(EXCLUDE_BIZ_DOMAIN)) {
                continue;
            }

            erpMessageClient.sendMarkdownMsg(MarkdownMsg.builder()
                    .title(TITLE)
                    .content(String.format(NOTIFY_PATTERN, project.getName()))
                    .receivers(Collections.singletonList(reachGoalReceiver)).build());
        }
        log.info("完成项目目标达成日期通知任务");
        return ReturnT.SUCCESS;
    }
}
