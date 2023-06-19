package com.timevale.forward.service.impl;

import cn.hutool.core.util.StrUtil;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandUpdateCondition;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProjectBizDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ProjectBizDemandService;
import com.timevale.forward.facade.api.request.BizDemandLinkProjectReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import generator.domain.ProjectBizDemandDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 业务需求、项目关联相关
 * 注意：目前该功能只适用于交付项目
 *
 * @author jingchun
 * created on 2023/4/17
 */
@Slf4j
@RestService
@RequiredArgsConstructor
public class ProjectBizDemandServiceImpl implements ProjectBizDemandService {

    private final ProjectMapper projectMapper;
    private final BizDemandMapper bizDemandMapper;
    private final BizDemandComponent bizDemandComponent;
    private final BizDemandLogComponent bizDemandLogComponent;
    private final ProjectBizDemandMapper projectBizDemandMapper;
    private final BizChangeLogMapper bizChangeLogMapper;
    private final PersonComponent personComponent;

    @Override
    public BaseResult<Void> linkOrUnlinkBizDemandProject(BizDemandLinkProjectReq bizDemandLinkProjectReq) {
        if (Objects.equals(bizDemandLinkProjectReq.getType(), LinkOrUnLinkEnum.LINK.getCode())) {
            return linkBizDemandProject(bizDemandLinkProjectReq);
        } else {
            return unlinkBizDemandProject(bizDemandLinkProjectReq);
        }
    }

    private BaseResult<Void> linkBizDemandProject(BizDemandLinkProjectReq bizDemandLinkProjectReq) {
        Long projectId = bizDemandLinkProjectReq.getProjectId();
        Set<Long> bizDemandIds = new HashSet<>(bizDemandLinkProjectReq.getBizDemandIds());
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "您关联的项目不存在");
        AssertUtil.checkState(ProjectStatusEnum.getByCode(project.getStatus()).isOngoing(),
                "项目已发布或者终止，无法关联业务需求");

        List<BizDemandDO> bizDemands = bizDemandMapper.getByIds(bizDemandIds);
        if (!bizDemandLinkProjectReq.isRemoveUnsatisfied()) {
            AssertUtil.checkState(bizDemands.size() == bizDemandIds.size(),
                    "您选择的业务需求已经不存在，请刷新页面后重试");
            AssertUtil.checkState(bizDemands.stream().map(BizDemandDO::getStatus)
                            .allMatch(status -> Objects.equals(status, BizDemandStatusEnum.RECEIVED.getCode())),
                    "您选择的业务需求存在未接收的状态，您只能选择已经接收的业务需求关联项目");
        }

        List<ProjectBizDemandDO> pbRelations =
                projectBizDemandMapper.selectByBizDemandIds(bizDemandIds);
        Set<Long> nonSourceBizDemandIds = bizDemands.stream().filter(bd ->
                        !Objects.equals(bd.getSourceId(), project.getSourceId()))
                .map(BizDemandDO::getId).collect(Collectors.toSet());

        if (!bizDemandLinkProjectReq.isRemoveUnsatisfied()) {
            AssertUtil.checkState(pbRelations.stream().map(ProjectBizDemandDO::getProjectId)
                            .allMatch(pId -> Objects.equals(projectId, pId)),
                    "您选择的业务需求已经关联到其他项目，无法再次关联");
            AssertUtil.checkState(nonSourceBizDemandIds.isEmpty(),
                    "您关联的业务需求和项目的关联交付项目不一致，请刷新后重试");
        }
        // 去除已经关联到该项目的列表
        Set<Long> alreadyRelatedBizDemandIds = pbRelations.stream()
                .map(ProjectBizDemandDO::getBizDemandId).collect(Collectors.toSet());
        bizDemandIds.removeIf(alreadyRelatedBizDemandIds::contains);
        bizDemandIds.removeIf(nonSourceBizDemandIds::contains);
        if (bizDemandIds.isEmpty()) {
            return BaseResult.success();
        }
        List<ProjectBizDemandDO> newPbRelations = bizDemandIds.stream()
                .map(bId -> new ProjectBizDemandDO().setProjectId(projectId).setBizDemandId(bId))
                .collect(Collectors.toList());

        bizDemands = bizDemands.stream().filter(bd -> bizDemandIds.contains(bd.getId())).collect(Collectors.toList());
        // 插入关联关系
        projectBizDemandMapper.batchInsert(newPbRelations);

        BizDemandStatusEnum status = bizDemandComponent.getBizDemandStatusByProjectStatus(project.getStatus());
        // 目前关联是项目一定为进行中状态
        bizDemandMapper.updateConditional(new BizDemandUpdateCondition().
                setStatus(status.getCode())
                .setProjectEndDate(project.getPlanEndDate())
                .setIds(bizDemandIds));

        String projectEndDate = DateUtil.parseToString(project.getPlanEndDate(), DateStyle.YYYY_MM_DD);
        List<BizChangeLogDO> logs = new ArrayList<>();
        for (Long bizDemandId : bizDemandIds) {
            BizChangeLogDO publishDateLog =
                    bizDemandLogComponent.buildLogWhenPublishDateChange(StringUtils.EMPTY, projectEndDate, bizDemandId);
            BizChangeLogDO statusLog =
                    bizDemandLogComponent.buildLogWhenStatusChange(BizDemandStatusEnum.RECEIVED.getText(),
                            status.getText(), bizDemandId);
            logs.add(publishDateLog);
            logs.add(statusLog);
        }
        // 日志
        bizDemandLogComponent.linkProject(project, bizDemands);
        if (!logs.isEmpty()) {
            bizChangeLogMapper.batchInsert(logs);
        }

        // 把sr添加到项目成员中
        Set<PersonAddReq> srs = bizDemands.stream()
                .filter(e -> StrUtil.isNotEmpty(e.getSrExpert()))
                .map(e -> new PersonAddReq(e.getSrExpert(), e.getSrExpertId()))
                .collect(Collectors.toSet());
        personComponent.addIfNotExisted(srs, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());

        return BaseResult.success();
    }

    private BaseResult<Void> unlinkBizDemandProject(BizDemandLinkProjectReq bizDemandUnlinkProjectReq) {
        Long projectId = bizDemandUnlinkProjectReq.getProjectId();
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "取消关联的项目不存在");
        List<Long> bizDemandIds = bizDemandUnlinkProjectReq.getBizDemandIds();
        List<ProjectBizDemandDO> pbRelations = projectBizDemandMapper.selectByProjectId(projectId);
        Set<Long> relBizDemandIds = pbRelations.stream().map(ProjectBizDemandDO::getBizDemandId)
                .collect(Collectors.toSet());
        bizDemandIds.removeIf(id -> !relBizDemandIds.contains(id));
        if (bizDemandIds.isEmpty()) {
            return BaseResult.success();
        }
        List<BizDemandDO> bizDemands = bizDemandMapper.getByIds(bizDemandIds);
        AssertUtil.notNull(bizDemands.isEmpty(), "取消关联的业务需求不存在");
        projectBizDemandMapper.delete(projectId, bizDemandIds);
        bizDemandMapper.updateConditional(new BizDemandUpdateCondition().
                setStatus(BizDemandStatusEnum.RECEIVED.getCode())
                .setProjectEndDateNull(true)
                .setIds(bizDemandIds));

        BizDemandStatusEnum status = bizDemandComponent.getBizDemandStatusByProjectStatus(project.getStatus());
        String projectEndDate = DateUtil.parseToString(project.getPlanEndDate(), DateStyle.YYYY_MM_DD);
        List<BizChangeLogDO> logs = new ArrayList<>();
        for (BizDemandDO bizDemand : bizDemands) {
            logs.add(bizDemandLogComponent.buildLogWhenPublishDateChange(projectEndDate, StringUtils.EMPTY,
                    bizDemand.getId()));
            logs.add(bizDemandLogComponent.buildLogWhenStatusChange(status.getText(),
                    BizDemandStatusEnum.RECEIVED.getText(), bizDemand.getId()));
        }
        // 日志
        bizDemandLogComponent.unlinkProject(project, bizDemands);
        bizChangeLogMapper.batchInsert(logs);
        return BaseResult.success();
    }

}
