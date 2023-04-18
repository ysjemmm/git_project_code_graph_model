package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProjectBizDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ProjectBizDemandService;
import com.timevale.forward.facade.api.request.BizDemandLinkProjectReq;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import generator.domain.ProjectBizDemandDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final BizDemandLogComponent bizDemandLogComponent;
    private final ProjectBizDemandMapper projectBizDemandMapper;

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
        AssertUtil.checkState(bizDemands.size() == bizDemandIds.size(),
                "您选择的业务需求已经不存在，请刷新页面后重试");
        AssertUtil.checkState(bizDemands.stream().map(BizDemandDO::getStatus)
                        .allMatch(status -> Objects.equals(status, BizDemandStatusEnum.RECEIVED.getCode())),
                "您选择的业务需求存在未接收的状态，您只能选择已经接收的业务需求关联项目");

        List<ProjectBizDemandDO> pbRelations =
                projectBizDemandMapper.selectByBizDemandIds(bizDemandIds);
        AssertUtil.checkState(pbRelations.stream().map(ProjectBizDemandDO::getProjectId)
                        .allMatch(pId -> Objects.equals(projectId, pId)),
                "您选择的业务需求已经关联到其他项目，无法再次关联");
        // 去除已经关联到该项目的列表
        Set<Long> alreadyRelatedBizDemandIds = pbRelations.stream()
                .map(ProjectBizDemandDO::getBizDemandId).collect(Collectors.toSet());
        bizDemandIds.removeIf(alreadyRelatedBizDemandIds::contains);
        if (bizDemandIds.isEmpty()) {
            return BaseResult.success();
        }
        List<ProjectBizDemandDO> newPbRelations = bizDemandIds.stream()
                .map(bId -> new ProjectBizDemandDO().setProjectId(projectId).setBizDemandId(bId))
                .collect(Collectors.toList());

        bizDemands = bizDemands.stream().filter(bd -> bizDemandIds.contains(bd.getId())).collect(Collectors.toList());
        projectBizDemandMapper.batchInsert(newPbRelations);

        // 日志
        bizDemandLogComponent.linkProject(project, bizDemands);

        return BaseResult.success();
    }

    private BaseResult<Void> unlinkBizDemandProject(BizDemandLinkProjectReq bizDemandUnlinkProjectReq) {
        Long bizDemandId = bizDemandUnlinkProjectReq.getBizDemandIds().get(0);
        Long projectId = bizDemandUnlinkProjectReq.getProjectId();
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "取消关联的项目不存在");
        BizDemandDO bizDemand = bizDemandMapper.get(bizDemandId);
        AssertUtil.notNull(bizDemand, "取消关联的业务需求不存在");
        List<ProjectBizDemandDO> pbRelations = projectBizDemandMapper.selectByProjectId(projectId);
        AssertUtil.checkState(pbRelations.stream().map(ProjectBizDemandDO::getBizDemandId)
                        .anyMatch(bId -> Objects.equals(bId, bizDemandId)),
                "不存在对应关联关系");
        projectBizDemandMapper.delete(projectId, bizDemandId);
        // 日志
        bizDemandLogComponent.unlinkProject(project, bizDemand);
        return BaseResult.success();
    }
}
