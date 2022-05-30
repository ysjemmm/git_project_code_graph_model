package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectComponentImpl implements ProjectComponent {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private ProjectRiskMapper projectRiskMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProjectNodeComponent projectNodeComponent;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private TestBillMapper testBillMapper;

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> page(ProjectListCondition condition, List<Long> projectIds) {
        // 查找产品经理
        if (CollectionUtils.isNotEmpty(condition.getPds())) {
            projectIds = personMapper.getMainIds(condition.getPds(), projectIds, PersonTypeEnum.PROJECT_PD.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        //团队成员
        if (CollectionUtils.isNotEmpty(condition.getTeamMembers())) {
            projectIds = personMapper.getMainIds(condition.getTeamMembers(), projectIds, PersonTypeEnum.PROJECT_MEMBER.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        //产品线业务域
        if (CollectionUtils.isNotEmpty(condition.getProductLineIds())
                || CollectionUtils.isNotEmpty(condition.getBizDomainIds())) {
            projectIds = projectMapper.getProjectIds(projectIds, condition.getProductLineIds(), condition.getBizDomainIds());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        if (condition.getReturnCountType() != null && condition.getReturnCount() != null) {
            projectIds = testBillMapper.getProjectIds(projectIds, condition.getReturnCountType(), condition.getReturnCount());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        if (condition.getIsDelay() != null) {
            projectIds = projectNodeMapper.getProjectIds(projectIds, condition.getIsDelay(), ProjectNodeEnum.SUBMIT_TEST.getText());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }

        buildConditionBeforeQuery(projectIds, condition);

        // 开始分页
        PageHelper.startPage(condition.getPageNum(), condition.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProjectListDO> projectDos = projectMapper.list(condition);

        // 筛选判空
        projectIds = projectDos.stream().map(ProjectListDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectIds)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        //2.填充人员信息
        Map<Long, List<PersonDO>> pdMap = personMapper.get(projectIds, PersonTypeEnum.PROJECT_PD.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));
        Map<Long, List<PersonDO>> teamMemberMap = personMapper.get(projectIds, PersonTypeEnum.PROJECT_MEMBER.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));

        //3.填充产品线/业务域信息
        Map<Long, List<ProjectProductLineBizDomain>> productLineMap = productLineMapper.getByProjectIds(projectIds)
                .stream().collect(Collectors.groupingBy(ProjectProductLineBizDomain::getProjectId));

        List<ProjectVO> projectVOList = ProjectCopier.INSTANCE.convert(projectDos);
        projectVOList.forEach(a -> {
            List<PersonDO> pds = pdMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(pds)) {
                String pdName = pds.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
                a.setPdName(pdName);
            }

            List<PersonDO> teamMembers = teamMemberMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(teamMembers)) {
                String teamMemberName = teamMembers.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
                a.setTeamMember(teamMemberName);
            }

            List<ProjectProductLineBizDomain> pdls = productLineMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(pdls)) {
                String productLineName = pdls.stream().map(ProjectProductLineBizDomain::getProductLineName).collect(Collectors.joining(","));
                a.setProductLineName(productLineName);
                String bizDomainName = pdls.stream().map(ProjectProductLineBizDomain::getBizDomainName).distinct().collect(Collectors.joining(","));
                a.setBizDomainName(bizDomainName);
            }
            a.setTypeName(ProjectTypeEnum.getTextByCode(a.getType()));
            a.setStatusName(ProjectStatusEnum.getTextByCode(a.getStatus()));
            a.setPriorityName(PriorityEnum.getTextByCode(a.getPriority()));
            a.setReturnCount(condition.getReturnCount());
            a.setIsDelay(condition.getIsDelay());
        });

        // 4.枚举值填充
        for (ProjectVO e : projectVOList) {
            e.setNodeStatusName(ProjectNodeStatusEnum.getNameByCode(e.getNodeStatus()));
        }

        // 5.是否需要预警
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectIdList(projectIds);
        Set<Long> riskSet = riskDOList.stream()
                .filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus()))
                .map(ProjectRiskDO::getProjectId)
                .collect(Collectors.toSet());
        for (ProjectVO e : projectVOList) {
            Integer status = e.getStatus();
            boolean warn = ProjectStatusEnum.SUSPEND.getCode().equals(status)
                    || ProjectStatusEnum.INVALID.getCode().equals(status)
                    || ProjectStatusEnum.RELEASED.getCode().equals(status);
            if (!warn) {
                e.setContainRisk(riskSet.contains(e.getId()));
            }
        }

        // 返回分页数据
        PageQueryResult<ProjectVO> pageQueryResult = new PageQueryResult<>();
        PageInfo<ProjectListDO> pageInfo = new PageInfo<>(projectDos);
        pageQueryResult.setResultList(projectVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public void fillInfo(List<ProjectNodeDO> projectNodes, ProjectDO projectDO) {
        // 计算项目状态,新逻辑
        Map<String, ProjectNodeDO> nodeMap = projectNodes.stream().collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        log.info("nodeMap={},,projectDO={}", nodeMap, projectDO);
        ProjectNodeDO demandStart = nodeMap.get(ProjectNodeEnum.START_PLAN.getText());
        ProjectNodeDO demandAudit = nodeMap.get(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText());
        ProjectNodeDO demandConstrue = nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE.getText());
        ProjectNodeDO demandConstrueReverse = nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText());
        Integer status = ProjectStatusEnum.WAITING.getCode();
        //规划中
        if (demandStart != null && demandStart.getActualDate() != null) {
            status = ProjectStatusEnum.PLANING.getCode();
        }
        // 研发中
        boolean dev = (demandStart == null || demandStart.getActualDate() != null)
                && (demandAudit == null || demandAudit.getActualDate() != null)
                && (demandConstrue == null || demandConstrue.getActualDate() != null)
                && (demandConstrueReverse == null || demandConstrueReverse.getActualDate() != null);
        if (dev) {
            status = ProjectStatusEnum.DEVING.getCode();
        }
        //测试中
        ProjectNodeDO review = nodeMap.get(ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText());
        ProjectNodeDO devStart = nodeMap.get(ProjectNodeEnum.DEVELOP_START.getText());
        ProjectNodeDO writeCase = nodeMap.get(ProjectNodeEnum.WRITE_TEST_CASES.getText());
        ProjectNodeDO reviewCase = nodeMap.get(ProjectNodeEnum.USE_CASE_REVIEW.getText());
        ProjectNodeDO submitTest = nodeMap.get(ProjectNodeEnum.SUBMIT_TEST.getText());
        boolean test = dev && (review == null || review.getActualDate() != null)
                && (devStart == null || devStart.getActualDate() != null)
                && (writeCase == null || writeCase.getActualDate() != null)
                && (reviewCase == null || reviewCase.getActualDate() != null)
                && (submitTest == null || submitTest.getActualDate() != null);
        if (test) {
            status = ProjectStatusEnum.TESTING.getCode();
        }

        //已发布
        ProjectNodeDO publishOfficial = nodeMap.get(ProjectNodeEnum.PUBLISH_OFFICIAL.getText());
        if (publishOfficial != null && publishOfficial.getActualDate() != null) {
            status = ProjectStatusEnum.RELEASED.getCode();
            //项目的实际完成时间
            projectDO.setActualEndDate(publishOfficial.getActualDate());
        }
        projectDO.setStatus(status);

        //计算项目实际开始时间：优先取需求阶段实际时间作为项目实际开始时间,若无,则取开发阶段第一个节点实际时间做为作为项目实际开始时间
        if (demandStart != null && demandStart.getActualDate() != null) {
            projectDO.setActualStartDate(demandStart.getActualDate());
        } else if (review != null && review.getActualDate() != null) {
            projectDO.setActualStartDate(review.getActualDate());
        } else if (devStart != null) {
            projectDO.setActualStartDate(devStart.getActualDate());
        }
        if (submitTest != null && submitTest.getActualDate() == null) {
            ProjectNodeDO oldSubmitTest = projectNodeMapper.getByName(projectDO.getId(), ProjectNodeEnum.SUBMIT_TEST.getText());
            if (oldSubmitTest != null && oldSubmitTest.getActualDate() != null) {
                throw new BaseBizRuntimeException("当前页面数据发生变化,请刷新后重试");
            }
        }
    }

    @Override
    public Integer getStatus(Long projectId) {
        // 查询项目节点
        List<ProjectNodeDO> nodeDOList = projectNodeComponent.get(projectId);

        // 节点排序
        ProjectNodeEnum.sort(nodeDOList);

        // 根据填入实际实际节点，判断项目状态
        Integer status = ProjectStatusEnum.RELEASED.getCode();
        for (ProjectNodeDO e : nodeDOList) {
            if (e.getActualDate() != null) {
                continue;
            }
            String name = e.getName();

            if (ProjectNodeEnum.START_PLAN.getText().equals(name)) {
                status = ProjectStatusEnum.WAITING.getCode();

            } else if (ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText().equals(name)
                    || ProjectNodeEnum.DEMAND_CONSTRUE.getText().equals(name)
                    || ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText().equals(name)) {
                status = ProjectStatusEnum.PLANING.getCode();
            } else if (ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText().equals(name)
                    || ProjectNodeEnum.DEVELOP_START.getText().equals(name)
                    || ProjectNodeEnum.WRITE_TEST_CASES.getText().equals(name)
                    || ProjectNodeEnum.USE_CASE_REVIEW.getText().equals(name)
                    || ProjectNodeEnum.SUBMIT_TEST.getText().equals(name)) {
                status = ProjectStatusEnum.DEVING.getCode();

            } else {
                status = ProjectStatusEnum.TESTING.getCode();
            }
            break;
        }
        return status;
    }

    @Override
    public void updateNodeStatus(Long projectId) {
        // 查询项目节点
        List<ProjectNodeDO> nodeDOList = projectNodeComponent.get(projectId);

        // 如果节点为空则状态设为待启动
        Integer nodeStatus;
        if (CollectionUtils.isEmpty(nodeDOList)) {
            nodeStatus = ProjectNodeStatusEnum.READY_START.getCode();
        } else {
            nodeStatus = ProjectNodeStatusEnum.getStatus(nodeDOList);
        }

        // 更新项目节点状态
        ProjectDO projectDO = projectMapper.get(projectId);
        projectDO.setNodeStatus(nodeStatus);
        projectMapper.update(projectDO);
    }

    @Override
    public List<Long> getLinkBizDemandIds(Long projectId) {
        if (projectId == null) {
            return Lists.emptyList();
        }
        List<Long> productDemandIds = projectProductDemandMapper.getByProjectId(projectId)
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return Lists.emptyList();
        }

        List<Long> bizDemandIds = productBizDemandMapper.selectByProductDemandIds(productDemandIds)
                .stream().map(ProductBizDemandDO::getBizDemandId).collect(Collectors.toList());
        log.info("项目:{},关联的有业务需求:{}", projectId, bizDemandIds);
        return bizDemandIds;
    }


    private void buildConditionBeforeQuery(List<Long> projectIds, ProjectListCondition condition) {
        condition.setIds(projectIds);
        condition.setName(StringUtil.toLikeStr(condition.getName()));
        condition.setPlanStartDateLeft(DateUtil.getStartOfDay(condition.getPlanStartDateLeft()));
        condition.setPlanStartDateRight(DateUtil.getEndOfDay(condition.getPlanStartDateRight()));
        condition.setPlanEndDateLeft(DateUtil.getStartOfDay(condition.getPlanEndDateLeft()));
        condition.setPlanEndDateRight(DateUtil.getEndOfDay(condition.getPlanEndDateRight()));
        condition.setActualStartDateLeft(DateUtil.getStartOfDay(condition.getActualStartDateLeft()));
        condition.setActualStartDateRight(DateUtil.getEndOfDay(condition.getActualStartDateRight()));
        condition.setActualEndDateLeft(DateUtil.getStartOfDay(condition.getActualEndDateLeft()));
        condition.setActualEndDateRight(DateUtil.getEndOfDay(condition.getActualEndDateRight()));
        condition.setCreateDateLeft(DateUtil.getStartOfDay(condition.getCreateDateLeft()));
        condition.setCreateDateRight(DateUtil.getEndOfDay(condition.getCreateDateRight()));
    }
}
