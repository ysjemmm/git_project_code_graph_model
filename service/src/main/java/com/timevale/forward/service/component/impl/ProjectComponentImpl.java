package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.condition.ProjectAcceptanceListCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.condition.ProjectNodeCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.facade.api.result.enums.ModifyCheckTypeEnum;
import com.timevale.forward.model.dto.ModifyProjectCheckDTO;
import com.timevale.forward.model.dto.ModifyProjectProcessedBundle;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.copy.ProjectNodeCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.query.QueryBase;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectComponentImpl implements ProjectComponent {

    @Resource
    private BizDemandMapper bizDemandMapper;
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
    private ProjectProductLineMapper projectProductLineMapper;
    @Resource
    private TestBillMapper testBillMapper;
    @Resource
    private SqlOrderComponent sqlOrderComponent;
    @Resource
    private BizLabelMapper bizLabelMapper;
    @Resource
    private BizLabelComponent bizLabelComponent;
    @Resource
    private ProjectLogComponent projectLogComponent;
    @Resource
    private CommonConfig config;
    @Resource
    private ProjectFlowMapper projectFlowMapper;
    @Resource
    private ProjectPbuMapper projectPbuMapper;
    @Resource
    private ProjectBizDomainMapper projectBizDomainMapper;
    @Resource
    private BizDemandComponent bizDemandComponent;
    @Resource
    private BizDomainMapper bizDomainMapper;
    @Resource
    private ProjectMilestoneActionMapper milestoneActionMapper;
    @Resource
    private ProjectGoalMapper projectGoalMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private ProjectNodeFlowMapper projectNodeFlowMapper;
    @Resource
    private ProjectAcceptanceMapper projectAcceptanceMapper;
    @Resource
    private ProjectPublishPlanComponent projectPublishPlanComponent;
    @Resource
    private BugOfflineMapper bugOfflineMapper;
    @Resource
    private BugLogMapper bugLogMapper;
    @Resource
    private ProjectDocumentComponent projectDocumentComponent;
    @Resource
    private ManDayMapper manDayMapper;
    @Resource
    private ProjectNodeRecordMapper projectNodeRecordMapper;
    @Resource
    private ProjectMemberEvaluateMapper memberEvaluateMapper;

    @Override
    public QueryResultVO<ProjectVO> page(ProjectListCondition condition, List<Long> projectIds) {

        // 查找产品经理
        if (CollUtil.isNotEmpty(condition.getPds())) {
            projectIds = personMapper.getMainIds(condition.getPds(), projectIds, PersonTypeEnum.PROJECT_PD.getCode());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }
        //团队成员
        if (CollUtil.isNotEmpty(condition.getTeamMembers())) {
            projectIds = personMapper.getMainIds(condition.getTeamMembers(), projectIds, PersonTypeEnum.PROJECT_MEMBER.getCode());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }
        //产研项目产品线业务域
        if (ProjectCategoryEnum.PRODUCT_PROJECT.getCode().equals(condition.getCategory())
                && (CollUtil.isNotEmpty(condition.getProductLineIds()) || CollectionUtils.isNotEmpty(condition.getBizDomainIds()))) {
            projectIds = projectMapper.getProjectIds(projectIds, condition.getProductLineIds(), condition.getBizDomainIds());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        // 内部项目业务域
        if (ProjectCategoryEnum.INNER_PROJECT.getCode().equals(condition.getCategory())
                && CollUtil.isNotEmpty(condition.getBizDomainIds())) {
            projectIds = projectBizDomainMapper.in(projectIds, condition.getBizDomainIds());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        // 内部项目pbu
        if (CollUtil.isNotEmpty(condition.getPbuIds())) {
            projectIds = projectPbuMapper.in(projectIds, condition.getPbuIds());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        //打回次数
        if (condition.getReturnCountType() != null && condition.getReturnCount() != null) {
            projectIds = testBillMapper.getProjectIds(projectIds, condition.getReturnCountType(), condition.getReturnCount());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        //提测实际时间
        ProjectNodeCondition c = ProjectNodeCondition.builder()
                .projectIds(projectIds)
                .nodeName(ProjectNodeEnum.SUBMIT_TEST.getText())
                .build();
        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.selectByCondition(c);
        if (condition.getActualTestDateLeft() != null && condition.getActualTestDateRight() != null) {
            projectIds = projectNodeDOList.stream()
                    .filter(e -> {
                        Date actualDate = e.getActualDate();
                        Date testDateLeft = condition.getActualTestDateLeft();
                        Date testDateRight = condition.getActualTestDateRight();
                        return actualDate != null
                                && testDateLeft.compareTo(actualDate) <= 0
                                && testDateRight.compareTo(actualDate) >= 0;
                    })
                    .map(ProjectNodeDO::getProjectId)
                    .collect(Collectors.toList());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        //是否逾期
        if (condition.getIsDelay() != null) {
            List<Long> tmpProjectIds = testBillMapper.getProjectIdsOfDelay(projectIds);
            if (condition.getIsDelay()) {
                projectIds = tmpProjectIds;
            } else if (CollUtil.isEmpty(projectIds)) {
                projectIds = projectMapper.getAllId();
                projectIds.removeAll(tmpProjectIds);
            } else {
                projectIds.removeAll(tmpProjectIds);
            }
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        // 是否包含风险
        if (BooleanUtil.isTrue(condition.getIncludeRisk())) {
            // 查询所有待处理的分享
            List<ProjectRiskDO> projectRiskDOList = projectRiskMapper.selectByProjectIdListStatus(projectIds, ProjectRiskStatusEnum.PENDING.getCode());
            // 过滤出有分享的项目id
            projectIds = projectRiskDOList.stream()
                    .map(ProjectRiskDO::getProjectId)
                    .distinct()
                    .collect(Collectors.toList());

            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }

            // 项目状态需要过滤掉已暂停、已作废、已发布、已结项
            List<Integer> status = condition.getStatus();
            if (CollUtil.isEmpty(status)) {
                for (ProjectStatusEnum e : ProjectStatusEnum.values()) {
                    status.add(e.getCode());
                }
            }
            status.remove(ProjectStatusEnum.SUSPEND.getCode());
            status.remove(ProjectStatusEnum.INVALID.getCode());
            status.remove(ProjectStatusEnum.RELEASED.getCode());
            status.remove(ProjectStatusEnum.CONCLUSION.getCode());

            if (CollUtil.isEmpty(status)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        //是否打标
        if (CollUtil.isNotEmpty(condition.getLabelIds())) {
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(condition.getLabelIds(), BizTypeEnum.PROJECT.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (condition.getContainLabel()) {
                if (CollUtil.isEmpty(projectIds)) {
                    projectIds = bizIds;
                } else {
                    projectIds.retainAll(bizIds);
                }
            } else {
                if (CollUtil.isEmpty(projectIds)) {
                    projectIds = projectMapper.getAllId();
                }
                projectIds.removeAll(bizIds);
            }

            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        if (condition.getNodeCode() != null) {
            String nodeName = ProjectNodeEnum.getNameByCode(condition.getNodeCode());
            Date startOfDay = DateUtil.getStartOfDay(condition.getActualDateLeft());
            Date endOfDay = DateUtil.getEndOfDay(condition.getActualDateRight());
            c = ProjectNodeCondition.builder()
                    .projectIds(projectIds).nodeName(nodeName).actualDateLeft(startOfDay).actualDateRight(endOfDay).build();
            projectIds = projectNodeMapper.selectByCondition(c).stream().map(ProjectNodeDO::getProjectId).collect(Collectors.toList());
            if (CollUtil.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        // 填充筛选项 —— 项目id列表
        condition.setIds(projectIds);

        // 产品线分析、排查
        List<ProductLineAnalyseVO> analyseVOList = analyse(condition);
        List<Long> conditionSubProductLineIdList = condition.getSubProductLineIds();
        if (CollUtil.isNotEmpty(conditionSubProductLineIdList)) {
            Set<Long> resultProductLineIdSet = analyseVOList.stream().map(ProductLineAnalyseVO::getProductLineId).collect(Collectors.toSet());
            List<Long> queryProductLineIdList = conditionSubProductLineIdList.stream().filter(resultProductLineIdSet::contains).collect(Collectors.toList());
            boolean pageEmpty = CollUtil.isEmpty(queryProductLineIdList);
            if (!pageEmpty) {
                projectIds = projectMapper.getProjectIds(projectIds, queryProductLineIdList, condition.getBizDomainIds());
                pageEmpty = CollUtil.isEmpty(projectIds);
            }
            if (pageEmpty) {
                QueryResultVO<ProjectVO> queryResultVO = new QueryResultVO<>();
                queryResultVO.setAnalyseVOList(analyseVOList);
                queryResultVO.setPageQueryResult(ResultUtil.pageEmpty());
                return queryResultVO;
            }
        }

        // 更新筛选项 —— 项目id列表，开始查询开始分页
        condition.setIds(projectIds);
        String collation = sqlOrderComponent.build(condition.getOrderFiled(), condition.getOrderCollation());
        PageHelper.startPage(condition.getPageNum(), condition.getPageSize(), collation);
        List<ProjectListDO> projectDos = projectMapper.list(condition);

        // 筛选判空
        projectIds = projectDos.stream().map(ProjectListDO::getId).collect(Collectors.toList());
        if (CollUtil.isEmpty(projectIds)) {
            return ResultUtil.queryResultEmpty();
        }

        Map<Long, List<BizLabelSimpleVO>> bizLabelMap =
                bizLabelComponent.getBizLabelMap(projectIds, BizTypeEnum.PROJECT.getCode());

        //填充人员信息
        Map<Long, List<PersonDO>> pdMap = personMapper.get(projectIds, PersonTypeEnum.PROJECT_PD.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));
        Map<Long, List<PersonDO>> teamMemberMap = personMapper.get(projectIds, PersonTypeEnum.PROJECT_MEMBER.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));

        //填充产品线/业务域信息
        Map<Long, List<ProjectProductLineBizDomain>> productLineMap = productLineMapper.getByProjectIds(projectIds)
                .stream().collect(Collectors.groupingBy(ProjectProductLineBizDomain::getProjectId));

        //提测实际时间
        Map<Long, List<ProjectNodeDO>> testNodeMap = projectNodeDOList.stream().collect(Collectors.groupingBy(ProjectNodeDO::getProjectId));

        //填充打回次,填充是否逾期
        Map<Long, List<TestBillDO>> testBillMap = testBillMapper.list(projectIds).stream().collect(Collectors.groupingBy(TestBillDO::getProjectId));

        // 转换
        List<ProjectVO> projectVOList = ProjectCopier.INSTANCE.convert(projectDos);

        // 结果项目id
        List<Long> projectIdList = projectVOList.stream().map(ProjectVO::getId).collect(Collectors.toList());

        // 项目节点
        List<ProjectNodeDO> nodeDOList = projectNodeMapper.selectByProjectIdList(projectIdList);
        Map<Long, List<ProjectNodeDO>> nodeMap = nodeDOList.stream().collect(Collectors.groupingBy(ProjectNodeDO::getProjectId));

        // 包含风险集合
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectIdList(projectIds);
        Set<Long> riskSet = riskDOList.stream()
                .filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus()))
                .map(ProjectRiskDO::getProjectId)
                .collect(Collectors.toSet());

        // 查询审批工作流
        List<ProjectFlowDO> conclusionFlows = projectFlowMapper.getByProjectIds(projectIdList,
                FlowTypeEnum.CONCLUSION.getCode(),
                ForwardFlowStatusEnum.AUDITING.getCode());
        Set<Long> conclusionFlowSet = conclusionFlows.stream().map(ProjectFlowDO::getProjectId).collect(Collectors.toSet());

        // pbu
        List<ProjectPbuDO> pjPbus = projectPbuMapper.getByProjectIds(projectIdList);
        Map<Long, List<ProjectPbuDO>> pjPbuGroup = pjPbus.stream().collect(Collectors.groupingBy(ProjectPbuDO::getProjectId));
        Set<Long> pbuIds = pjPbus.stream().map(ProjectPbuDO::getPbuId).collect(Collectors.toSet());
        Map<Long, GroupResponse> pbuMap = bizDemandComponent.getGroupListTreeMap(pbuIds);

        // 业务域
        List<ProjectBizDomainDO> pjBds = projectBizDomainMapper.getByProjectIds(projectIds);
        Map<Long, List<ProjectBizDomainDO>> pjBdGroup = pjBds.stream().collect(Collectors.groupingBy(ProjectBizDomainDO::getProjectId));
        List<BizDomainDO> bizDomainDOS = bizDomainMapper.selectAllBizDomain();
        Map<Long, BizDomainDO> bizDomainDOMap = Maps.uniqueIndex(bizDomainDOS, BaseDO::getId);

        // 遍历填充数据
        for (ProjectVO projectVO : projectVOList) {
            List<PersonDO> pds = pdMap.get(projectVO.getId());
            if (CollUtil.isNotEmpty(pds)) {
                String pdName = pds.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
                projectVO.setPdName(pdName);

                List<String> pdIdList = pds.stream()
                        .map(PersonDO::getUserId)
                        .collect(Collectors.toList());
                projectVO.setPdId(pdIdList);
            }

            List<PersonDO> teamMembers = teamMemberMap.get(projectVO.getId());
            if (CollUtil.isNotEmpty(teamMembers)) {
                String teamMemberName = teamMembers.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
                projectVO.setTeamMember(teamMemberName);
            }

            List<ProjectProductLineBizDomain> pdls = productLineMap.get(projectVO.getId());
            if (CollUtil.isNotEmpty(pdls)) {
                String productLineName = pdls.stream().map(ProjectProductLineBizDomain::getProductLineName).collect(Collectors.joining(","));
                projectVO.setProductLineName(productLineName);
                String bizDomainName = pdls.stream().map(ProjectProductLineBizDomain::getBizDomainName).distinct().collect(Collectors.joining(","));
                projectVO.setBizDomainName(bizDomainName);
            }

            List<TestBillDO> testBillDos = testBillMap.get(projectVO.getId());
            if (CollUtil.isEmpty(testBillDos)) {
                projectVO.setReturnCount(0);
                projectVO.setIsDelay(false);
            } else {
                projectVO.setReturnCount(testBillDos.get(0).getReturnCount());
                projectVO.setIsDelay(testBillDos.get(0).getDelayDay() > 0);
            }
            projectVO.setActualTestDate(CollUtil.isEmpty(testNodeMap.get(projectVO.getId())) ? null : testNodeMap.get(projectVO.getId()).get(0).getActualDate());

            // 项目节点状态、节点计划时间
            Integer nodeStatus = projectVO.getNodeStatus();
            projectVO.setNodeStatusName(ProjectNodeStatusEnum.getNameByCode(nodeStatus));
            projectVO.setNodePlanDate(projectNodeComponent.getRecentPlanDate(nodeMap.get(projectVO.getId())));

            // 是否需要预警
            Integer status = projectVO.getStatus();
            boolean warn = ProjectStatusEnum.SUSPEND.getCode().equals(status)
                    || ProjectStatusEnum.INVALID.getCode().equals(status)
                    || ProjectStatusEnum.RELEASED.getCode().equals(status)
                    || ProjectStatusEnum.CONCLUSION.getCode().equals(status);
            if (!warn) {
                projectVO.setContainRisk(riskSet.contains(projectVO.getId()));
            }

            List<BizLabelSimpleVO> labelSimpleVOList = bizLabelMap.get(projectVO.getId());
            if (CollUtil.isNotEmpty(labelSimpleVOList)) {
                projectVO.setLabelNames(labelSimpleVOList);
            }

            // 内部项目作废文案处理(临时)
            if (ProjectStatusEnum.INVALID.getCode().equals(projectVO.getStatus())
                    && ProjectCategoryEnum.INNER_PROJECT.getCode().equals(projectVO.getCategory())) {
                projectVO.setStatusName(CommonConstant.INVALID);
            }

            // pbu
            Optional.ofNullable(pjPbuGroup.get(projectVO.getId()))
                    .map(e -> e.stream()
                            .map(ProjectPbuDO::getPbuId)
                            .map(pbuMap::get)
                            .filter(Objects::nonNull)
                            .map(GroupResponse::getGroupName)
                            .collect(Collectors.joining(",")))
                    .ifPresent(projectVO::setPbuNames);
            // 业务域
            Optional.ofNullable(pjBdGroup.get(projectVO.getId()))
                    .map(e -> e.stream()
                            .map(ProjectBizDomainDO::getBizDomainId)
                            .map(bizDomainDOMap::get)
                            .filter(Objects::nonNull)
                            .map(BizDomainDO::getName)
                            .collect(Collectors.joining(",")))
                    .ifPresent(projectVO::setBizDomainName);

            // 是否存在审批中的结项流程
            projectVO.setConclusionAuditing(conclusionFlowSet.contains(projectVO.getId()));
        }

        // 项目子项目数量
        if (Objects.equals(condition.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode())) {
            List<ProjectChildCountDO> projectChildCounts = projectMapper.countChildren(projectIds);
            Map<Long, ProjectChildCountDO> countById =
                    Maps.uniqueIndex(projectChildCounts, ProjectChildCountDO::getId);
            for (ProjectVO projectVO : projectVOList) {
                ProjectChildCountDO count = countById.get(projectVO.getId());
                if (count != null) {
                    projectVO.setChildrenCount(count.getChildCount());
                }
            }
        }

        // 分页数据
        PageQueryResult<ProjectVO> pageQueryResult = new PageQueryResult<>();
        PageInfo<ProjectListDO> pageInfo = new PageInfo<>(projectDos);
        pageQueryResult.setResultList(projectVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        QueryResultVO<ProjectVO> queryResultVO = new QueryResultVO<>();
        queryResultVO.setPageQueryResult(pageQueryResult);
        queryResultVO.setAnalyseVOList(analyseVOList);

        return queryResultVO;
    }

    @Override
    public void fillInfo(List<ProjectNodeDO> newProjectNodes, ProjectDO projectDO) {
        // 计算项目状态,新逻辑
        Map<String, ProjectNodeDO> nodeMap = newProjectNodes.stream().collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        log.info("nodeMap={},,projectDO={}", nodeMap, projectDO);
        ProjectNodeDO demandStart = nodeMap.get(ProjectNodeEnum.START_PLAN.getText());
        ProjectNodeDO demandAudit = nodeMap.get(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText());
        ProjectNodeDO demandConstrue = nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE.getText());
        ProjectNodeDO demandConstrueReverse = nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText());
        ProjectNodeDO demandUedAudit = nodeMap.get(ProjectNodeEnum.UED_AUDIT.getText());
        Integer status = ProjectStatusEnum.WAITING.getCode();
        //规划中
        if (demandStart != null && demandStart.getActualDate() != null) {
            status = ProjectStatusEnum.PLANING.getCode();
        }
        // 研发中
        boolean dev = (demandStart == null || demandStart.getActualDate() != null)
                && (demandAudit == null || demandAudit.getActualDate() != null)
                && (demandConstrue == null || demandConstrue.getActualDate() != null)
                && (demandConstrueReverse == null || demandConstrueReverse.getActualDate() != null)
                && (demandUedAudit == null || demandUedAudit.getActualDate() != null);

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
    }

    @Override
    public Integer getStatus(Long projectId) {
        // 查询项目节点
        List<ProjectNodeDO> nodeDOList = projectNodeComponent.get(projectId);

        // 节点排序
        nodeDOList = projectNodeComponent.sort(nodeDOList);

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
                    || ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText().equals(name)
                    || ProjectNodeEnum.UED_AUDIT.getText().equals(name)) {
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
        if (CollUtil.isEmpty(nodeDOList)) {
            nodeStatus = ProjectNodeStatusEnum.READY_START.getCode();
        } else {
            nodeStatus = projectNodeComponent.getStatus(nodeDOList);
        }

        // 更新项目节点状态
        ProjectDO projectDO = new ProjectDO();
        projectDO.setId(projectId);
        projectDO.setNodeStatus(nodeStatus);
        projectMapper.update(projectDO);
    }

    @Override
    public List<Long> getLinkProductDemandIds(Long projectId) {
        if (projectId == null) {
            return Lists.emptyList();
        }
        List<Long> productDemandIds = projectProductDemandMapper.getByProjectId(projectId)
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productDemandIds)) {
            return Lists.emptyList();
        }
        log.info("项目:{},关联的有产品需求:{}", projectId, productDemandIds);
        return productDemandIds;
    }

    @Override
    public List<ProjectDO> pageAllOngoingProjects(QueryBase queryBase) {
        PageHelper.startPage(queryBase.getPageNum(), queryBase.getPageSize());

        List<ProjectDO> projectDOList = projectMapper.pageAllOngoingProjects();

        PageInfo<ProjectDO> pageInfo = new PageInfo<>(projectDOList);

        return pageInfo.getList();
    }

    @Override
    public void attachChildProject(ProjectDO parent, ProjectDO child) {
        String prepend = parent.getParentIds().substring(0, parent.getParentIds().length() - 1);
        String prefixRegexp = "^" + child.getParentIds();
        projectMapper.attachChildProject(prepend, prefixRegexp);
        projectLogComponent.addAppendChildLog(parent.getId(), child.getName());
        projectLogComponent.addAttachParentLog(child.getId(), parent.getName());
    }

    @Override
    public void deleteChildProject(ProjectDO parent, ProjectDO child) {
        List<ProjectDO> childList = projectMapper.selectByParentIdsRegexp("^" + child.getParentIds());
        if (childList.isEmpty()) {
            return;
        }
        List<Long> childIds = childList.stream().map(ProjectDO::getId).collect(Collectors.toList());

        // 删除里成本关联关系
        if (CollUtil.isNotEmpty(childList)) {
            milestoneActionMapper.delByRelates(childIds, MilestoneTypeEnum.PROJECT.getCode());
        }

        projectMapper.deleteChildren(parent.getParentIds().length(), "^" + child.getParentIds());
        projectLogComponent.addDeleteChildLog(parent.getId(), child.getName());
        projectLogComponent.addDetachParentLog(child.getId(), parent.getName());
    }

    @Override
    public String getUrl(Long projectId) {
        return String.format(config.getCommonViewUrl(), TabEnum.PROJECT_MANAGEMENT.getText(), projectId);
    }

    private List<ProductLineAnalyseVO> analyse(ProjectListCondition condition) {
        List<ProjectListDO> projectListDOList = projectMapper.list(condition);
        List<Long> projectIdList = projectListDOList.stream().map(BaseDO::getId).collect(Collectors.toList());

        // 项目关联的产品线
        List<ProjectProductLineDO> projectProductLineDOList = projectProductLineMapper.getByProjectIdList(projectIdList);
        List<Long> productLineIdList = projectProductLineDOList.stream().map(ProjectProductLineDO::getProductLineId).distinct().collect(Collectors.toList());
        List<ProductLineDO> productLineDOList = productLineMapper.getByIds(productLineIdList);

        // 产品线id-名称 map
        Map<Long, String> productLineMap = productLineDOList.stream().collect(Collectors.toMap(BaseDO::getId, ProductLineDO::getName, (a, b) -> a));
        Map<Long, Integer> productLineCount = productLineDOList.stream().collect(Collectors.toMap(BaseDO::getId, e -> 0, (a, b) -> a));

        // 统计个数
        for (ProjectProductLineDO e : projectProductLineDOList) {
            Long productLineId = e.getProductLineId();
            Integer count = productLineCount.get(productLineId);
            productLineCount.put(productLineId, count + 1);
        }

        List<ProductLineAnalyseVO> analyseVOList = new ArrayList<>();
        productLineCount.forEach((k, v) -> {
            String name = productLineMap.get(k);
            ProductLineAnalyseVO analyseVO = new ProductLineAnalyseVO();
            analyseVO.setCount(v);
            analyseVO.setProductLineId(k);
            analyseVO.setProductLineName(name);
            analyseVOList.add(analyseVO);
        });

        // 逆序排序
        analyseVOList.sort((a, b) -> b.getCount().compareTo(a.getCount()));

        return analyseVOList;
    }

    @Override
    public ProjectDO getByBizDemandId(Long bizDemandId) {
        if (bizDemandId == null) {
            return null;
        }
        return bizDemandMapper.getByBizDemandId(bizDemandId);
    }

    @Override
    public void updatePbu(Long projectId, Collection<Long> newPbuIds) {
        if (CollUtil.isEmpty(newPbuIds)) {
            newPbuIds = Collections.emptyList();
        }

        List<ProjectPbuDO> pjPbus = projectPbuMapper.getByProjectId(projectId);
        List<Long> oldPbuIds = pjPbus.stream().map(ProjectPbuDO::getPbuId).collect(Collectors.toList());

        Collection<Long> addPubIds = CollUtil.subtract(newPbuIds, oldPbuIds);
        Collection<Long> delPubIds = CollUtil.subtract(oldPbuIds, newPbuIds);

        if (CollUtil.isNotEmpty(addPubIds)) {
            projectPbuMapper.batchAdd(projectId, addPubIds);
        }
        if (CollUtil.isNotEmpty(delPubIds)) {
            projectPbuMapper.batchDel(projectId, delPubIds);
        }
    }

    @Override
    public void updateBizDomain(Long projectId, Collection<Long> newBdIds) {
        if (CollUtil.isEmpty(newBdIds)) {
            newBdIds = Collections.emptyList();
        }

        List<ProjectBizDomainDO> pjBds = projectBizDomainMapper.getByProjectId(projectId);
        List<Long> oldBdIds = pjBds.stream().map(ProjectBizDomainDO::getBizDomainId).collect(Collectors.toList());

        Collection<Long> addBdIds = CollUtil.subtract(newBdIds, oldBdIds);
        Collection<Long> delBdIds = CollUtil.subtract(oldBdIds, newBdIds);

        if (CollUtil.isNotEmpty(addBdIds)) {
            projectBizDomainMapper.batchAdd(projectId, addBdIds);
        }
        if (CollUtil.isNotEmpty(delBdIds)) {
            projectBizDomainMapper.batchDel(projectId, delBdIds);
        }
    }

    @Override
    public ModifyProjectProcessedBundle checkProjectModify(ProjectModifyReq projectModifyReq,
                                                           Consumer<ModifyProjectCheckDTO> dataHandler) {
        final Long projectId = projectModifyReq.getId();
        ProjectDO oldProject = projectMapper.get(projectId);
        // 没法继续校验的地方直接 assert 抛出异常
        AssertUtil.notNull(oldProject, "修改的项目不存在");
        AssertUtil.checkState(!ProjectStatusEnum.terminated(oldProject.getStatus()),
                "项目处于发布或中止中，不可编辑，请刷新后重试");

        ProjectDO newProject = ProjectCopier.INSTANCE.convert(projectModifyReq);
        List<ProjectNodeDO> nodes = ProjectNodeCopier.INSTANCE.convert(projectModifyReq.getProjectNodes());
        List<Runnable> delayTasks = new ArrayList<>();

        //有流程,计划时间不能变
        Integer delayType = projectModifyReq.getDelayType();
        if (!DelayTypeEnum.NONE.getCode().equals(delayType)) {
            newProject.setPlanStartDate(oldProject.getPlanStartDate());
            newProject.setPlanEndDate(oldProject.getPlanEndDate());
        }

        // 校验项目名称重复
        if (!Objects.equals(oldProject.getName(), projectModifyReq.getName()) &&
                projectMapper.getByName(projectModifyReq.getName()) != null) {
            dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                    "该项目名称已存在，请修改后重试"));
        }

        // 校验项目目标
        if (YesOrNoEnum.YES.getCode().equals(projectModifyReq.getIsWithGoal()) &&
                projectGoalMapper.getByProjectId(projectId).isEmpty()) {
            dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                    "项目含有项目目标，请至少添加一条项目目标数据"));
        }

        Map<String, ProjectNodeDO> nodeMap = nodes.stream()
                .collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        // 检查任务
        Set<String> nodeKeys = nodeMap.keySet();
        if (Stream.of(ProjectNodeEnum.START_PLAN.getText(),
                ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText(),
                ProjectNodeEnum.DEMAND_CONSTRUE.getText(),
                ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText(),
                ProjectNodeEnum.UED_AUDIT.getText()).anyMatch(nodeKeys::contains)) {
            //删除需求规划阶段时需要校验是否有关联任务,若有关联待执行&进行中&已完成&已暂停的任务,不能删除
            if (taskMapper.getByProjectId(projectId).stream()
                    .anyMatch(a -> ProjectStageEnum.DEMAND.getCode().equals(a.getStage())
                            && !TaskStatusEnum.INVALID.getCode().equals(a.getStatus()))) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                        "需求规划阶段已关联任务，不可删除"));
            }
        }
        // 计算项目状态
        ProjectNodeDO publishNode = nodeMap.get(ProjectNodeEnum.PUBLISH_OFFICIAL.getText());
        boolean released = publishNode != null && publishNode.getActualDate() != null;
        if (released) {
            if (ProjectStatusEnum.SUSPEND.getCode().equals(oldProject.getStatus())) {
                // 编辑项目
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                        "项目状态为暂停时，不能填写发布正式的实际时间"));
            }
            if (nodes.stream().map(ProjectNodeDO::getActualDate).anyMatch(Objects::isNull)) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                        "请填写完其他节点的实际时间后，再填写发布正式的实际时间"));
            } else {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY,
                        "项目节点的实际时间已全部填入，状态将变为已发布，已发布的项目不可再编辑。"));
            }
        }

        if (!Objects.equals(oldProject.getPjEstablishPublishDate(), newProject.getPjEstablishPublishDate()) &&
                projectNodeFlowMapper.getByProjectId(projectId).stream()
                        .anyMatch(a -> ForwardFlowStatusEnum.AUDITING.getCode().equals(a.getStatus()))) {
            dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                    "发布正式节点流程处于审核中，不能修改立项预期上线时间"));
        }


        if (YesOrNoEnum.NO.getCode().equals(newProject.getIsAcceptance()) &&
                CollectionUtils.isNotEmpty(projectAcceptanceMapper.list(ProjectAcceptanceListCondition.builder()
                        .status(Lists.newArrayList(ForwardFlowStatusEnum.AUDITING.getCode(),
                                ForwardFlowStatusEnum.COMPLETE.getCode(),
                                ForwardFlowStatusEnum.REJECT.getCode()))
                        .projectId(projectId).build()))) {
            dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ACCEPTANCE, true,
                    "存在验收流程，不能将项目验收改为否"));
        }

        if (released && YesOrNoEnum.YES.getCode().equals(newProject.getIsAcceptance())) {
            ProjectAcceptanceListCondition c = ProjectAcceptanceListCondition.builder().projectId(newProject.getId()).build();
            List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);
            boolean allWithdraw = list.stream().allMatch(a -> ForwardFlowStatusEnum.WITHDRAW.getCode().equals(a.getStatus()));
            if (CollectionUtils.isEmpty(list) || allWithdraw) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ACCEPTANCE, true,
                        "您还没有发起项目验收,请验收通过后再发布"));
            }
            list = list.stream().filter(a -> !ForwardFlowStatusEnum.WITHDRAW.getCode().equals(a.getStatus()))
                    .collect(Collectors.toList());
            Map<String, List<ProjectAcceptanceDO>> groupMap = list.stream()
                    .collect(Collectors.groupingBy(ProjectAcceptanceDO::getAcceptorId));
            groupMap.forEach((k, v) -> {
                List<ProjectAcceptanceDO> order = v.stream().sorted(
                                Comparator.comparing(ProjectAcceptanceDO::getCreateDate).reversed())
                        .collect(Collectors.toList());
                ProjectAcceptanceDO last = order.get(0);
                //去除已撤回的验收,最新一条不是已通过 不能发布
                if (ForwardFlowStatusEnum.AUDITING.getCode().equals(last.getStatus()) ||
                        ForwardFlowStatusEnum.REJECT.getCode().equals(last.getStatus())) {
                    dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ACCEPTANCE, true,
                            "请确保所有验收人员验收通过后再发布"));
                }
            });
        }
        if (released && Objects.equals(newProject.getIsPlatformPublish(), 1)) {
            if (!projectPublishPlanComponent.linkPublishPlan(newProject.getId())) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.PUBLISH, true,
                        "请关联发布计划"));
            }
            if (projectPublishPlanComponent.anyMatchNotFinished(newProject.getId())) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.PUBLISH, true,
                        "您的发布计划还未结束，请前往发布平台处理"));
            }
        }
        List<ProjectNodeDO> oldNodes = projectNodeMapper.get(projectId);
        Map<String, ProjectNodeDO> oldNodeMap = oldNodes.stream()
                .collect(Collectors.toMap(ProjectNodeDO::getName, a -> a, (v1, v2) -> v1));
        //找出可以发起审批的节点
        List<ProjectNodeDO> startFlowNodes = nodes.stream().filter(a -> ProjectNodeEnum.canStartFlow(a.getName()))
                .collect(Collectors.toList());
        List<ProjectFlowDO> projectFlowDOList = projectFlowMapper.getByProjectId(projectId);
        List<Integer> flowTypes = projectFlowDOList.stream().filter(a ->
                        !ForwardFlowStatusEnum.PRE_EDIT.getCode().equals(a.getStatus()))
                .map(ProjectFlowDO::getFlowType).collect(Collectors.toList());
        startFlowNodes.forEach(a -> {
            //有流程,实际时间不能修改
            if (flowTypes.contains(ProjectNodeEnum.getCodeByName(a.getName())) && oldNodeMap.containsKey(a.getName())
                    && !Objects.equals(oldNodeMap.get(a.getName()).getActualDate(), a.getActualDate())) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                        String.format("%s节点存在审批流程，不能修改实际时间", a.getName())));
            }
        });

        // 项目发布时需要校验未关闭bug
        if (released
                // 注意 checkProductRelease 方法里包含更新线下 bug 的逻辑（延迟执行）
                && !checkProductRelease(projectModifyReq.getId(), delayTasks)) {
            dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.BUG_OFFLINE, true,
                    "该项目还有bug未关闭，请关闭后再发布"));
        }

        ProjectNodeDO submitTest = nodeMap.get(ProjectNodeEnum.SUBMIT_TEST.getText());
        if (submitTest != null) {
            ProjectNodeDO oldSubmitTest = projectNodeMapper.getByName(projectId, ProjectNodeEnum.SUBMIT_TEST.getText());
            TestBillDO oldTestBillDO = testBillMapper.selectByProjectId(projectId);
            if (submitTest.getActualDate() == null && oldSubmitTest != null &&
                    oldSubmitTest.getActualDate() != null && oldTestBillDO != null) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                        "提测后，不能修改提测节点的实际时间"));
            }

            if (oldTestBillDO != null && TestBillStatusEnum.TEST_SUCCESS.getCode().equals(oldTestBillDO.getStatus())
                    && oldSubmitTest != null && !Objects.equals(submitTest.getPlanDate(), oldSubmitTest.getPlanDate())) {
                // 提测已经通过,修改计划时间,重算逾期时长
                TestBillDO testBillDO = new TestBillDO();
                if (submitTest.getActualDate().after(submitTest.getPlanDate())) {
                    String planDate = DateUtil.parseToString(submitTest.getPlanDate(), DateFormatConst.DATE_FORMAT);
                    String actualDate = DateUtil.parseToString(submitTest.getActualDate(), DateFormatConst.DATE_FORMAT);
                    testBillDO.setDelayDay(DateUtil.getIntervalDays(planDate, actualDate));
                } else {
                    testBillDO.setDelayDay(0);
                }
                testBillDO.setProjectId(projectId);
                // 校验结束后再执行
                delayTasks.add(() -> testBillMapper.updateDelayDay(testBillDO, false));
            }
        }

        if (released) {
            // 仅发布节点需要校验
            List<String> result = projectDocumentComponent.docNeedFillIn(projectId,
                    nodes, newProject.getType());
            if (!result.isEmpty()) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.DOCUMENT,
                        String.join("，", result) +
                                "未维护，请在项目文档中按要求维护。若无文档，请维护原因说明。"));
            }
            List<ManDayDO> manDayDOList = manDayMapper.getByProjectId(projectId);
            if (manDayDOList.isEmpty()) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.MAN_DAY,
                        "项目成员的人天明细数据未维护，请在项目人天中维护。"));
            }
        }
        boolean containsBase = projectNodeRecordMapper.contain(projectId);
        if (containsBase) {
            // 有基线版本校验
            if (nodes.stream().anyMatch(n -> n.getPlanDate() == null)) {
                dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                        "需要维护好所有项目节点的计划时间"));
            } else {
                if (DelayTypeEnum.SUBMIT_TEST_DELAY.getCode().equals(delayType)) {
                    dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY,
                            "提测计划时间发生了延迟，会生成新的版本。"));
                } else if (DelayTypeEnum.PUBLISH_DELAY.getCode().equals(delayType)) {
                    if (released) {
                        dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY,
                                "当前项目发布计划时间发生变更，需要变更流程通过才可以维护发布实际时间。"));
                    }
                }
            }
        } else {
            // 无基线版本校验
            ProjectNodeDO developStartNode = nodeMap.get(ProjectNodeEnum.DEVELOP_START.getText());
            if (developStartNode != null && developStartNode.getActualDate() != null) {
                if (nodes.stream().anyMatch(n -> n.getPlanDate() == null)) {
                    dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY, true,
                            "已开始开发的项目需要维护好所有项目节点的计划时间"));
                } else {
                    dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.ANY,
                            "开发开始实际时间维护完成后，计划工作量生成基线版本，生成项目原始积分。"));
                }
            } else {
                List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.getByProjectId(projectId);
                if (memberEvaluateDOList.stream().filter(ProjectMemberEvaluateDO::getIncludeStat)
                        .anyMatch(m -> m.getPlanWorkload() == null ||
                                m.getPlanWorkload().compareTo(BigDecimal.ZERO) == 0)) {
                    // 未填写工作量
                    dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.PROJECT_POINT, true,
                            "存在纳入积分考核的成员计划工作量未录入的情况，请将计划工作量数据维护完整后，才可以保存开发开始实际时间"));
                } else {
                    // 已经填写工作量
                    dataHandler.accept(new ModifyProjectCheckDTO(ModifyCheckTypeEnum.PROJECT_POINT,
                            "开发开始实际时间维护完成后，计划工作量生成基线版本，生成项目原始积分，确认继续保存吗？"));
                }
            }
        }


        return new ModifyProjectProcessedBundle(oldProject, newProject, nodes, delayTasks);
    }


    private boolean checkProductRelease(Long projectId, List<Runnable> delayTasks) {
        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByProjectId(projectId);

        List<BugOfflineDO> releaseList = bugOfflineDOList.stream()
                .filter(e -> BugStatusEnum.canRelease(e.getStatus()))
                .collect(Collectors.toList());
        // 如果不仅为完成、关闭、延期修复，返回报错
        if (releaseList.size() != bugOfflineDOList.size()) {
            return false;
        }

        List<BugOfflineDO> postponeList = releaseList.stream()
                .filter(e -> BugStatusEnum.POSTPONE_REPAIR.getCode().equals(e.getStatus()))
                .collect(Collectors.toList());

        // 断开关联关系，并且记录bug日志
        if (!CollectionUtils.isEmpty(postponeList)) {
            ProjectDO projectDO = projectMapper.get(projectId);

            List<BugLogDO> bugLogDOList = Lists.newArrayList();
            postponeList.forEach(e -> {
                BugLogDO bugLogDO = new BugLogDO();
                bugLogDO.setField(BugFieldEnum.PROJECTS.getText());
                bugLogDO.setOldValue(projectDO.getName());
                bugLogDO.setNewValue(CommonConstant.NULL);
                bugLogDO.setMainId(e.getId());
                bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
                bugLogDOList.add(bugLogDO);
            });

            delayTasks.add(() -> {
                bugLogMapper.batchInsert(bugLogDOList);
                bugOfflineMapper.unlinkBugOffline(postponeList);
            });
        }

        return true;
    }

}
