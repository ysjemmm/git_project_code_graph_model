package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
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
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private TaskComponent taskComponent;


    @Override
    public BaseResult<PageQueryResult<ProjectVO>> page(ProjectListCondition condition, List<Long> projectIds, boolean isList) {
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
        buildConditionBeforeQuery(projectIds, condition);
        PageHelper.startPage(condition.getPageNum(), condition.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProjectListDO> projectDO = projectMapper.list(condition);
        projectIds = projectDO.stream().map(ProjectListDO::getId).collect(Collectors.toList());
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

        List<ProjectVO> projectVO = ProjectCopier.INSTANCE.convert(projectDO);
        Map<Long, Boolean> warning = isNeedWarningOnProject(projectVO, isList);
        projectVO.forEach(a -> {
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
            a.setWarning(warning.get(a.getId()));
        });
        PageQueryResult<ProjectVO> pageQueryResult = new PageQueryResult<>();
        PageInfo<ProjectListDO> pageInfo = new PageInfo<>(projectDO);
        pageQueryResult.setResultList(projectVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);

    }

    @Override
    public void fillInfo(List<ProjectNodeDO> projectNodes, ProjectDO projectDO) {
        // 计算项目状态
        Map<String, ProjectNodeDO> nodeMap = projectNodes.stream().collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        log.info("nodeMap={},,projectDO={}", nodeMap, projectDO);
        ProjectNodeDO demandStart = nodeMap.get(ProjectNodeEnum.START_PLAN.getProjectNodeName());
        ProjectNodeDO demandAudit = nodeMap.get(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getProjectNodeName());
        ProjectNodeDO demandConstrue = nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE.getProjectNodeName());
        Integer status = ProjectStatusEnum.WAITING.getCode();
        //规划中
        if (demandStart != null && demandStart.getActualDate() != null) {
            status = ProjectStatusEnum.PLANING.getCode();
        }
        // 研发中
        boolean dev = (demandStart == null || demandStart.getActualDate() != null)
                && (demandAudit == null || demandAudit.getActualDate() != null)
                && (demandConstrue == null || demandConstrue.getActualDate() != null);
        if (dev) {
            status = ProjectStatusEnum.DEVING.getCode();
        }
        //测试中
        ProjectNodeDO review = nodeMap.get(ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getProjectNodeName());
        ProjectNodeDO devStart = nodeMap.get(ProjectNodeEnum.DEVELOP_START.getProjectNodeName());
        ProjectNodeDO writeCase = nodeMap.get(ProjectNodeEnum.WRITE_TEST_CASES.getProjectNodeName());
        ProjectNodeDO reviewCase = nodeMap.get(ProjectNodeEnum.USE_CASE_REVIEW.getProjectNodeName());
        ProjectNodeDO submitTest = nodeMap.get(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName());
        boolean test = (review == null || review.getActualDate() != null)
                && (devStart == null || devStart.getActualDate() != null)
                && (writeCase == null || writeCase.getActualDate() != null)
                && (reviewCase == null || reviewCase.getActualDate() != null)
                && (submitTest == null || submitTest.getActualDate() != null);
        if (test) {
            status = ProjectStatusEnum.TESTING.getCode();
        }

        //已发布
        ProjectNodeDO publishOfficial = nodeMap.get(ProjectNodeEnum.PUBLISH_OFFICIAL.getProjectNodeName());
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
        } else {
            projectDO.setActualStartDate(devStart.getActualDate());
        }
    }

    private Map<Long, Boolean> isNeedWarningOnProject(List<ProjectVO> projectVOList, boolean isList) {
        Map<Long, Boolean> warning = new HashMap<>();
        if (!isList) {
            return warning;
        }
        List<Integer> filterStatus = Arrays.asList(ProjectStatusEnum.WAITING.getCode(), ProjectStatusEnum.PLANING.getCode()
                , ProjectStatusEnum.DEVING.getCode(), ProjectStatusEnum.TESTING.getCode());
        List<Long> ids = projectVOList.stream().filter(a -> filterStatus.contains(a.getStatus())).map(ProjectVO::getId).collect(Collectors.toList());
        Map<Long, List<ProjectNodeDO>> nodeMapping = projectNodeMapper.getByProjectIds(ids).stream().collect(Collectors.groupingBy(ProjectNodeDO::getProjectId));
        for (Long pid : nodeMapping.keySet()) {
            // 1若延期时长=实际时间(取最大时间)-计划完成时间＞0,节点逾期预警
            ProjectNodeDO node = nodeMapping.get(pid).stream().filter(a -> a.getPlanDate() != null && a.getActualDate() != null)
                    .max(Comparator.comparing(ProjectNodeDO::getActualDate)).orElse(null);
            if (node != null && node.getActualDate().after(node.getPlanDate())) {
                warning.put(pid, true);
                continue;
            }

            node = nodeMapping.get(pid).stream().filter(a -> a.getActualDate() == null && a.getPlanDate() != null)
                    .min(Comparator.comparing(ProjectNodeDO::getPlanDate)).orElse(null);
            // 2节点没有实际时间，延期时长=当前时间-计划完成时间≥2个工作日,逾期未录入预警
            if (node != null) {
                Date currentDate = DateUtil.parseToDate(DateUtil.parseToString(new Date(), DateFormatConst.DATE_FORMAT));
                Date planDate = DateUtil.parseToDate(DateUtil.parseToString(node.getPlanDate(), DateFormatConst.DATE_FORMAT));
                log.info("无实际时间,计划时间最小的节点,node:{}", node);
                if (currentDate.after(planDate)) {
                    BigDecimal elapsedTime = taskComponent.getElapsedTime(planDate, currentDate);
                    if (elapsedTime.compareTo(new BigDecimal("16")) >= 0) {
                        warning.put(pid, true);
                        continue;
                    }
                }
            }
            warning.put(pid, false);
        }
        return warning;
    }

//    private Map<Long, String> displayNodeOnEachProject(List<ProjectVO> projectVOList, boolean isList) {
//        Map<Long, String> displayNode = new HashMap<>();
//        if (!isList) {
//            return displayNode;
//        }
//        List<Long> ids = projectVOList.stream().map(ProjectVO::getId).collect(Collectors.toList());
//        Map<Long, List<ProjectNodeDO>> nodeMapping = projectNodeMapper.getByProjectIds(ids)
//                .stream().collect(Collectors.groupingBy(ProjectNodeDO::getProjectId));
//        for (Long pid : nodeMapping.keySet()) {
//            Map<String, ProjectNodeDO> nodeMap = nodeMapping.get(pid).stream().filter(a -> a.getActualDate() == null)
//                    .collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
//            String nodeName = null;
//            if (nodeMap.containsKey(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.START_PLAN.getProjectNodeName())) {
//                //开始规划有实际时间,需求内审无实际时间
//                nodeName = ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.DEMAND_CONSTRUE.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getProjectNodeName())) {
//                //需求内审有实际时间,需求串讲无实际时间
//                nodeName = ProjectNodeEnum.DEMAND_CONSTRUE.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.DEMAND_CONSTRUE.getProjectNodeName())) {
//                //需求串讲有实际时间,详设无实际时间
//                nodeName = ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.DEVELOP_START.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getProjectNodeName())) {
//                //详设有实际时间,开发开始无实际时间
//                nodeName = ProjectNodeEnum.DEVELOP_START.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.WRITE_TEST_CASES.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.DEVELOP_START.getProjectNodeName())) {
//                //开发开始有实际时间,编写测试用例无实际时间
//                nodeName = ProjectNodeEnum.WRITE_TEST_CASES.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.USE_CASE_REVIEW.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.WRITE_TEST_CASES.getProjectNodeName())) {
//                //编写测试用例有实际时间,用例评审无实际时间
//                nodeName = ProjectNodeEnum.USE_CASE_REVIEW.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.USE_CASE_REVIEW.getProjectNodeName())) {
//                //用例评审有实际时间,提测无实际时间
//                nodeName = ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.TEST_START.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName())) {
//                //提测有实际时间,测试开始无实际时间
//                nodeName = ProjectNodeEnum.TEST_START.getProjectNodeName();
//            } else if (nodeMap.containsKey(ProjectNodeEnum.PUBLISH_SIMULATE.getProjectNodeName()) && !nodeMap.containsKey(ProjectNodeEnum.TEST_START.getProjectNodeName())) {
//                //测试开始有实际时间,发布模拟无实际时间
//                nodeName = ProjectNodeEnum.PUBLISH_SIMULATE.getProjectNodeName();
//            } else if (!nodeMap.containsKey(ProjectNodeEnum.PUBLISH_SIMULATE.getProjectNodeName())) {
//                //发布模拟有实际时间
//                nodeName = ProjectNodeEnum.PUBLISH_OFFICIAL.getProjectNodeName();
//            }
//            displayNode.put(pid, nodeName);
//        }
//        return displayNode;
//    }

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
