package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.condition.ProjectNodeCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectMilestoneService;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.query.QueryBase;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
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
    private ProjectMilestoneMapper projectMilestoneMapper;

    @Resource
    private ProjectMilestoneService milestoneService;

    @Override
    public QueryResultVO<ProjectVO> page(ProjectListCondition condition, List<Long> projectIds) {
        // 查找产品经理
        if (CollectionUtils.isNotEmpty(condition.getPds())) {
            projectIds = personMapper.getMainIds(condition.getPds(), projectIds, PersonTypeEnum.PROJECT_PD.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }
        //团队成员
        if (CollectionUtils.isNotEmpty(condition.getTeamMembers())) {
            projectIds = personMapper.getMainIds(condition.getTeamMembers(), projectIds, PersonTypeEnum.PROJECT_MEMBER.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }
        //产品线业务域
        if (CollectionUtils.isNotEmpty(condition.getProductLineIds())
                || CollectionUtils.isNotEmpty(condition.getBizDomainIds())) {
            projectIds = projectMapper.getProjectIds(projectIds, condition.getProductLineIds(), condition.getBizDomainIds());
            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }
        //打回次数
        if (condition.getReturnCountType() != null && condition.getReturnCount() != null) {
            projectIds = testBillMapper.getProjectIds(projectIds, condition.getReturnCountType(), condition.getReturnCount());
            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        //提测实际时间
        ProjectNodeCondition c = ProjectNodeCondition.builder().projectIds(projectIds).nodeName(ProjectNodeEnum.SUBMIT_TEST.getText()).build();
        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.selectByCondition(c);
        if (condition.getActualTestDateLeft() != null && condition.getActualTestDateRight() != null) {
            Date startOfDay = DateUtil.getStartOfDay(condition.getActualTestDateLeft());
            Date endOfDay = DateUtil.getEndOfDay(condition.getActualTestDateRight());
            projectIds = projectNodeDOList.stream().filter(a -> a.getActualDate() != null && a.getActualDate().after(startOfDay) && a.getActualDate().before(endOfDay))
                    .map(ProjectNodeDO::getProjectId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }
        //是否逾期
        if (condition.getIsDelay() != null) {
            List<Long> tmpProjectIds = testBillMapper.getProjectIdsOfDelay(projectIds);
            if (condition.getIsDelay()) {
                projectIds = tmpProjectIds;
            } else if (CollectionUtils.isEmpty(projectIds)) {
                projectIds = projectMapper.getAllId();
                projectIds.removeAll(tmpProjectIds);
            } else {
                projectIds.removeAll(tmpProjectIds);
            }
            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        // 是否包含风险
        if (condition.getIncludeRisk() != null && condition.getIncludeRisk()) {
            List<ProjectRiskDO> projectRiskDOList = projectRiskMapper.selectByProjectIdListStatus(projectIds, ProjectRiskStatusEnum.PENDING.getCode());
            projectIds = projectRiskDOList.stream().map(ProjectRiskDO::getProjectId).distinct().collect(Collectors.toList());

            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }

            //项目状态≠已暂停、已作废、已发布
            List<Integer> status = condition.getStatus();
            if (CollectionUtils.isEmpty(status)) {
                for (ProjectStatusEnum e : ProjectStatusEnum.values()) {
                    status.add(e.getCode());
                }
            }
            status.removeIf(e -> ProjectStatusEnum.SUSPEND.getCode().equals(e)
                    || ProjectStatusEnum.INVALID.getCode().equals(e)
                    || ProjectStatusEnum.RELEASED.getCode().equals(e));
            if (CollectionUtils.isEmpty(status)) {
                return ResultUtil.queryResultEmpty();
            }
        }

        //是否打标
        if (CollectionUtils.isNotEmpty(condition.getLabelIds())) {
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(condition.getLabelIds(), BizTypeEnum.PROJECT.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (condition.getContainLabel()) {
                if (CollectionUtils.isEmpty(projectIds)) {
                    projectIds = bizIds;
                } else {
                    projectIds.retainAll(bizIds);
                }
            } else {
                if (CollectionUtils.isEmpty(projectIds)) {
                    projectIds = projectMapper.getAllId();
                }
                projectIds.removeAll(bizIds);
            }

            if (CollectionUtils.isEmpty(projectIds)) {
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
            if (CollectionUtils.isEmpty(projectIds)) {
                return ResultUtil.queryResultEmpty();
            }
        }
        buildConditionBeforeQuery(projectIds, condition);

        // 产品线分析
        List<ProductLineAnalyseVO> analyseVOList = analyse(condition);

        // 产品线排查
        List<Long> conditionSubProductLineIdList = condition.getSubProductLineIds();
        if (CollectionUtils.isNotEmpty(conditionSubProductLineIdList)) {
            Set<Long> resultProductLineIdSet = analyseVOList.stream().map(ProductLineAnalyseVO::getProductLineId).collect(Collectors.toSet());
            List<Long> queryProductLineIdList = conditionSubProductLineIdList.stream().filter(resultProductLineIdSet::contains).collect(Collectors.toList());
            boolean pageEmpty = CollectionUtils.isEmpty(queryProductLineIdList);
            if (!pageEmpty) {
                projectIds = projectMapper.getProjectIds(projectIds, queryProductLineIdList, condition.getBizDomainIds());
                pageEmpty = CollectionUtils.isEmpty(projectIds);
            }
            if (pageEmpty) {
                QueryResultVO<ProjectVO> queryResultVO = new QueryResultVO<>();
                queryResultVO.setAnalyseVOList(analyseVOList);
                queryResultVO.setPageQueryResult(ResultUtil.pageEmpty());
                return queryResultVO;
            }
        }

        condition.setIds(projectIds);
        // 开始分页
        String collation = sqlOrderComponent.build(condition.getOrderFiled(), condition.getOrderCollation());
        PageHelper.startPage(condition.getPageNum(), condition.getPageSize(), collation);
        List<ProjectListDO> projectDos = projectMapper.list(condition);

        // 筛选判空
        projectIds = projectDos.stream().map(ProjectListDO::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectIds)) {
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

        for (ProjectVO a : projectVOList) {
            List<PersonDO> pds = pdMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(pds)) {
                String pdName = pds.stream().map(PersonDO::getUserName).collect(Collectors.joining(","));
                a.setPdName(pdName);

                List<String> pdIdList = pds.stream()
                        .map(PersonDO::getUserId)
                        .collect(Collectors.toList());
                a.setPdId(pdIdList);
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
            a.setLevelName(ProjectLevelEnum.getTextByCode(a.getLevel()));

            List<TestBillDO> testBillDos = testBillMap.get(a.getId());
            if (CollectionUtils.isEmpty(testBillDos)) {
                a.setReturnCount(0);
                a.setIsDelay(false);
            } else {
                a.setReturnCount(testBillDos.get(0).getReturnCount());
                a.setIsDelay(testBillDos.get(0).getDelayDay() > 0);
            }
            a.setActualTestDate(CollectionUtils.isEmpty(testNodeMap.get(a.getId())) ? null : testNodeMap.get(a.getId()).get(0).getActualDate());

            // 项目节点状态、节点计划时间
            Integer nodeStatus = a.getNodeStatus();
            a.setNodeStatusName(ProjectNodeStatusEnum.getNameByCode(nodeStatus));
            a.setNodePlanDate(projectNodeComponent.getRecentPlanDate(nodeMap.get(a.getId())));

            // 是否需要预警
            Integer status = a.getStatus();
            boolean warn = ProjectStatusEnum.SUSPEND.getCode().equals(status)
                    || ProjectStatusEnum.INVALID.getCode().equals(status)
                    || ProjectStatusEnum.RELEASED.getCode().equals(status);
            if (!warn) {
                a.setContainRisk(riskSet.contains(a.getId()));
            }

            List<BizLabelSimpleVO> labelSimpleVOList = bizLabelMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(labelSimpleVOList)) {
                a.setLabelNames(labelSimpleVOList);
            }
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
        if (CollectionUtils.isEmpty(nodeDOList)) {
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
    }

    @Override
    public void deleteChildProject(ProjectDO parent, ProjectDO child) {
        List<ProjectDO> childList = projectMapper.selectByParentIdsRegexp("^" + child.getParentIds());
        List<Long> childIds = childList.stream().map(ProjectDO::getId).collect(Collectors.toList());
        List<ProjectMilestone> milestones = projectMilestoneMapper.selectByProjectIds(childIds);
        if (!milestones.isEmpty()) {
            milestones.stream().map(ProjectMilestone::getId).forEach(milestoneService::deleteMilestone);
        }
        projectMapper.deleteChildren(parent.getParentIds().length(), "^" + child.getParentIds());
    }

    private List<ProductLineAnalyseVO> analyse(ProjectListCondition condition) {
        List<ProjectListDO> projectListDOList = projectMapper.list(condition);
        List<Long> projectIdList = projectListDOList.stream().map(BaseDO::getId).collect(Collectors.toList());

        // 项目关联的产品线
        List<ProjectProductLineDO> projectProductLineDOList = projectProductLineMapper.getByProjectIdList(projectIdList);
        List<Long> productLineIdList = projectProductLineDOList.stream().map(ProjectProductLineDO::getProductLineId).distinct().collect(Collectors.toList());
        List<ProductLineDO> productLineDOList = productLineMapper.selectByIds(productLineIdList);

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
