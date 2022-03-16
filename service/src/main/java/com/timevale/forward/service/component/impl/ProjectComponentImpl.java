package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
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


    @Override
    public BaseResult<PageQueryResult<ProjectVO>> page(ProjectListCondition condition,List<Long> projectIds) {
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
        buildConditionBeforeQuery(projectIds,condition);
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
        Map<String, ProjectNodeDO> nodeMap = projectNodes
                .stream()
                .collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        log.info("nodeMap={},,projectDO={}", nodeMap, projectDO);
        ProjectNodeDO node = null;
        if ((node = nodeMap.get(ProjectStageEnum.TEST_RELEASE.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.RELEASED.getCode());
            projectDO.setActualEndDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.TEST_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.TESTING.getCode());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_REVIEW.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.DEVING.getCode());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.DEVING.getCode());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEMAND_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.PLANING.getCode());
        } else {
            projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
        }
        //计算项目实际开始时间：优先取需求阶段实际时间作为项目实际开始时间,若无,则取开发阶段第一个节点实际时间做为作为项目实际开始时间
        if ((node = nodeMap.get(ProjectStageEnum.DEMAND_START.getText())) != null) {
            projectDO.setActualStartDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_REVIEW.getText())) != null) {
            projectDO.setActualStartDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_START.getText())) != null) {
            projectDO.setActualStartDate(node.getActualDate());
        }
    }

    private void buildConditionBeforeQuery(List<Long>projectIds,ProjectListCondition condition){
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
    }
}
