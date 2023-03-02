package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.ProjectAcceptanceListCondition;
import com.timevale.forward.dal.condition.ProjectListChildCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectMilestoneService;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.query.ProjectLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectPageQuery;
import com.timevale.forward.facade.api.query.ProjectProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.event.ProjectCreateEvent;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.ProjectEstablishDateChangeMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private PersonComponent personComponent;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectNodeComponent projectNodeComponent;
    @Resource
    private ProjectProductLineComponent projectProductLineComponent;
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private ProjectProductDemandComponent projectProductDemandComponent;
    @Resource
    private ProductDemandComponent productDemandComponent;
    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private ProductLineMapper productLineMapper;
    @Resource
    private ProjectComponent projectComponent;
    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;
    @Resource
    private ProductBizDemandMapper productBizDemandMapper;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private TaskComponent taskComponent;
    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;
    @Resource
    private TaskProductDemandMapper taskProductDemandMapper;
    @Resource
    private BugOfflineMapper bugOfflineMapper;
    @Resource
    protected BugLogMapper bugLogMapper;
    @Resource
    private BugOfflineComponent bugOfflineComponent;
    @Resource
    private ProjectLogComponent projectLogComponent;
    @Resource
    private ProductDemandLogComponent productDemandLogComponent;
    @Resource
    private ProjectFlowMapper projectFlowMapper;
    @Resource
    private ProjectPublishPlanComponent projectPublishPlanComponent;
    @Resource
    private BizDemandComponent bizDemandComponent;
    @Resource
    private BizChangeLogMapper bizChangeLogMapper;
    @Resource
    private ProjectGoalMapper projectGoalMapper;
    @Resource
    private ProjectNodeFlowComponent projectNodeFlowComponent;
    @Resource
    private ProjectNodeFlowMapper projectNodeFlowMapper;
    @Resource
    private ProjectNodeRecordMapper projectNodeRecordMapper;
    @Resource
    private MessageEventPublisher messageEventPublisher;
    @Resource
    private CustomDemandComponent customDemandComponent;
    @Resource
    private BizDemandMapper bizDemandMapper;
    @Resource
    private LabelComponent labelComponent;
    @Resource
    private BizLabelComponent bizLabelComponent;
    @Resource
    private BizLabelMapper bizLabelMapper;
    @Resource
    private ProjectAcceptanceMapper projectAcceptanceMapper;
    @Resource
    private ManDayReportComponent manDayReportComponent;
    @Resource
    private ProjectProductLineMapper projectProductLineMapper;
    @Resource
    private ProjectNodeMapper projectNodeMapper;
    @Resource
    private TestBillMapper testBillMapper;
    @Resource
    private ProjectDocumentComponent projectDocumentComponent;
    @Resource
    private ProjectBudgetMapper projectBudgetMapper;
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectMilestoneService projectMilestoneService;
    @Resource
    private ProjectMilestoneMapper projectMilestoneMapper;
    @Resource
    private UserComponent userComponent;
    @Resource
    private InnerProjectStatusUpdateComponent innerProjectStatusUpdateComponent;
    @Resource
    private ProjectMilestoneComponent projectMilestoneComponent;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public BaseResult<QueryResultVO<ProjectVO>> list(ProjectQueryList projectQueryList) {
        log.info("项目列表接收参数:{}", projectQueryList);
        String currentUser = LocalSessionUtils.getUserInfo().getId();
        ProjectListCondition condition = ProjectCopier.INSTANCE.convert(projectQueryList);
        condition.setPageNum(projectQueryList.getPageNum());
        condition.setPageSize(projectQueryList.getPageSize());
        List<Long> projectIds = new ArrayList<>();
        //1.查找我或我的团队所属项目id
        if (AscriptionEnum.CURRENT_USER.name().equals(projectQueryList.getAscription())) {
            projectIds = personMapper.getMainIds(Lists.newArrayList(currentUser), null, PersonTypeEnum.PROJECT_MEMBER.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }

        } else if (AscriptionEnum.TEAM.name().equals(projectQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser, true);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            projectIds = personMapper.getMainIds(allMyStaffWithSelf, null, PersonTypeEnum.PROJECT_MEMBER.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }
        }

        if (CollectionUtils.isNotEmpty(projectQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(projectQueryList.getLabelCategoryIds())) {
            List<Long> labelIds = labelComponent.getLabelIds(projectQueryList.getLabelIds(), projectQueryList.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(labelIds) && projectQueryList.getContainLabel()) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }
            condition.setLabelIds(labelIds);
        }
        return BaseResult.success(projectComponent.page(condition, projectIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(ProjectUpdateStatusReq req) {
        Long projectId = req.getProjectId();
        Integer type = req.getType();
        String suspendReason = req.getSuspendReason();
        String invalidReason = req.getInvalidReason();
        log.info("项目暂停或作废接收参数:{},{}", projectId, type);
        if (!ProjectStatusEnum.SUSPEND.getCode().equals(type)
                && !ProjectStatusEnum.INVALID.getCode().equals(type)) {
            throw new BaseBizRuntimeException("操作类型不是暂停或作废,请重试输入");
        }
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        Integer oldStatus = projectDO.getStatus();
        if (ProjectStatusEnum.INVALID.getCode().equals(oldStatus) || ProjectStatusEnum.RELEASED.getCode().equals(oldStatus)) {
            throw new BaseBizRuntimeException("项目状态为已作废或已发布时,不能修改状态");
        }

        // 更新项目状态
        ProjectDO updateStatusDO = new ProjectDO();
        updateStatusDO.setId(projectId);
        updateStatusDO.setStatus(type);
        updateStatusDO.setSuspendReason(suspendReason);
        updateStatusDO.setInvalidReason(invalidReason);
        projectMapper.update(updateStatusDO);

        //修改产品需求状态
        productDemandComponent.updateProductDemandStatus(projectId, type);
        if (ProjectStatusEnum.INVALID.getCode().equals(type)) {
            // 作废解除关联
            projectProductDemandComponent.update(projectId, null);

            bizLabelComponent.deleteLabel(projectDO.getId(), BizTypeEnum.PROJECT.getCode());
            // 删除子项目关联关系
            if (Objects.equals(projectDO.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode())) {
                List<ProjectDO> children = projectMapper.selectByParentIdsRegexp("^" + projectDO.getParentIds() + ".");
                for (ProjectDO child : children) {
                    projectComponent.deleteChildProject(projectDO, child);
                }
            }
            // 作废项目更新里程碑内部项目状态
            innerProjectStatusUpdateComponent.updateFromProject(projectDO);
        }
        String action = ProjectStatusEnum.SUSPEND.getCode().equals(type) ?
                ButtonActionEnum.SUSPEND.getText() : ButtonActionEnum.INVALID.getText();
        projectLogComponent.addLogWhenStatusChange(oldStatus, type, projectId, action);

        //记录暂停/作废原因更新日志
        String field = ProjectStatusEnum.SUSPEND.getCode().equals(type) ?
                BizChangeLogFieldEnum.SUSPEND_REASON.getText() : BizChangeLogFieldEnum.INVALID_REASON.getText();
        String reason = ProjectStatusEnum.SUSPEND.getCode().equals(type) ?
                suspendReason : invalidReason;
        projectLogComponent.addLogWhenContentChange(CommonConstant.NULL, reason, projectId, field);
        // 更新任务状态
        taskComponent.updateStatusAsProjectStatusChange(projectId, type, false);
        if (ProjectStatusEnum.SUSPEND.getCode().equals(type)) {
            projectMilestoneComponent.addMilestoneSuspendLog(projectId, MilestoneTypeEnum.PROJECT.getCode());
        } else {
            projectMilestoneComponent.addMilestoneInvalidLog(projectId, MilestoneTypeEnum.PROJECT.getCode());
        }
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> enable(Long projectId, Boolean enableTask) {
        log.info("项目开启接收参数:projectId={}", projectId);
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        if (!ProjectStatusEnum.SUSPEND.getCode().equals(projectDO.getStatus())) {
            throw new BaseBizRuntimeException("项目状态不是暂停,不能开启");
        }
        Integer oldStatus = projectDO.getStatus();
        String oldReason = projectDO.getSuspendReason();
        projectDO.setSuspendReason(null);

        List<ProjectNodeDO> projectNode = projectNodeComponent.get(projectId);
        log.info("项目开启,节点信息:projectNode={}", projectNode);
        if (CollectionUtils.isEmpty(projectNode)) {
            projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
            if (Objects.equals(projectDO.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode())) {
                projectDO.setStatus(innerProjectStatusUpdateComponent.calcProjectStatus(projectDO.getId()));
            }
            projectMapper.fullUpdateById(projectDO);
        } else {
            fillInfoWhenEnable(projectNode, projectDO);
        }
        // 更新任务状态
        taskComponent.updateStatusAsProjectStatusChange(projectId, projectDO.getStatus(), enableTask);

        projectLogComponent.addLogWhenStatusChange(oldStatus, projectDO.getStatus(), projectId, ButtonActionEnum.ENABLE.getText());
        //记录开启日志更新日志
        if (StringUtils.isNotBlank(oldReason)) {
            projectLogComponent.addLogWhenContentChange(oldReason, CommonConstant.NULL, projectId,
                    BizChangeLogFieldEnum.SUSPEND_REASON.getText());
        }
        projectMilestoneComponent.addMilestoneEnableLog(projectId, MilestoneTypeEnum.PROJECT.getCode());
        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(projectId);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectAddReq projectAddReq) {
        log.info("项目新增接收参数:{}", projectAddReq);
        ProjectDO project = projectMapper.getByName(projectAddReq.getName());

        if (projectAddReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("项目名称中请勿包含空格");
        }
        if (project != null) {
            throw new BaseBizRuntimeException("该项目名称已存在,请修改后重试");
        }
        if (YesOrNoEnum.YES.getCode().equals(projectAddReq.getIsWithGoal())) {
            AssertUtil.notEmpty(projectAddReq.getProjectGoals(), "项目含有项目目标，请至少添加一条项目目标数据");
            AssertUtil.checkState(projectAddReq.getProjectGoals().stream()
                    .filter(goal -> YesOrNoEnum.YES.getCode().equals(goal.getIsMain()))
                    .count() == 1, "项目目标主目标只能有一个，请检查参数");
            for (ProjectGoalAddReq projectGoal : projectAddReq.getProjectGoals()) {
                if (ProjectGoalTypeEnum.QUANTIFY.getCode().equals(projectGoal.getType())) {
                    AssertUtil.notNull(projectGoal.getReachValue(), "定量项目目标的目标达标值必填");
                } else {
                    projectGoal.setReachValue(null);
                }
            }
        }
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectAddReq);
        projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
        projectMapper.insert(projectDO);
        projectDO.setParentIds(Collections.singletonList(projectDO.getId()));

        //标签
        if (CollectionUtils.isNotEmpty(projectAddReq.getLabelIds())) {
            bizLabelComponent.addLabel(projectDO.getId(), projectAddReq.getLabelIds(), BizTypeEnum.PROJECT.getCode());
            bizLabelComponent.addLog(projectDO.getId(), projectAddReq.getLabelIds(), BizTypeEnum.PROJECT.getCode(), true);
        }

        // 产品线
        projectProductLineComponent.add(projectDO.getProductLineIds(), projectDO.getId());

        // 产品经理
        personComponent.add(projectAddReq.getPds(), projectDO.getId(), PersonTypeEnum.PROJECT_PD.getCode());
        List<String> pdUserIds = projectAddReq.getPds().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());

        // 父级项目
        if (projectAddReq.getParentId() != null) {
            ProjectDO parentProject = projectMapper.get(projectAddReq.getParentId());
            AssertUtil.notNull(parentProject, "父级项目不存在，请检查参数");
            projectComponent.attachChildProject(parentProject, projectDO);
        }

        // 团队成员
        List<PersonAddReq> teamMembers = projectAddReq.getTeamMembers();
        //过滤掉重复选择的项目经理,产品经理
        teamMembers = teamMembers.stream().filter(a -> !a.getUserId().equals(projectDO.getPmId()) && !pdUserIds.contains(a.getUserId()))
                .collect(Collectors.toList());
        teamMembers.addAll(projectAddReq.getPds());
        if (!pdUserIds.contains(projectAddReq.getPm().getUserId())) {
            //产品经理不包含项目经理时,将项目经理加入团队中
            teamMembers.add(projectAddReq.getPm());
        }
        personComponent.add(teamMembers, projectDO.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());
        //生成节点信息
        projectNodeComponent.buildDefaultNode(projectDO.getPlanStartDate(), projectDO.getPlanEndDate(), projectDO.getId());

        Integer status = ProjectStatusEnum.WAITING.getCode();
        projectLogComponent.addLogWhenStatusChange(status, status, projectDO.getId(), ButtonActionEnum.SUBMIT.getText());

        // 项目目标信息插入
        if (YesOrNoEnum.YES.getCode().equals(projectAddReq.getIsWithGoal())) {
            for (ProjectGoalAddReq goal : projectAddReq.getProjectGoals()) {
                goal.setProjectId(projectDO.getId());
            }
            projectGoalMapper.batchInsert(ProjectGoalCopier.INSTANCE.convert(projectAddReq.getProjectGoals()));
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> innerAdd(ProjectInnerAddReq projectInnerAddReq) {
        // 校验名称参数
        projectNameValidate(projectInnerAddReq.getName());

        // 转换，配置项目类型，有效阶段
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectInnerAddReq);

        // 项目落库
        projectMapper.innerInsert(projectDO);
        projectDO.setParentIds(Collections.singletonList(projectDO.getId()));

        // 获取项目id
        Long projectId = projectDO.getId();

        // 核心成员
        List<PersonAddReq> teamMembers = projectInnerAddReq.getTeamMembers();
        personComponent.duplicateRemove(teamMembers, projectInnerAddReq.getPm());
        personComponent.add(teamMembers, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.CORE.getCode());

        // 扩展成员
        List<PersonAddReq> extTeamMembers = projectInnerAddReq.getExtTeamMembers();
        personComponent.add(extTeamMembers, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.EXTENSION.getCode());

        // 父级项目
        if (projectInnerAddReq.getParentId() != null) {
            ProjectDO parentProject = projectMapper.get(projectInnerAddReq.getParentId());
            AssertUtil.notNull(parentProject, "父级项目不存在，请检查参数");
            projectComponent.attachChildProject(parentProject, projectDO);
        }

        // 添加项目目标信息
        List<ProjectGoalAddReq> projectGoals = projectInnerAddReq.getProjectGoals();
        if (CollUtil.isNotEmpty(projectGoals)) {
            // 转换
            List<ProjectGoalDO> projectGoalDOs = projectGoals.stream()
                    .map(e -> ProjectGoalCopier.INSTANCE.convert(e, projectId)).collect(Collectors.toList());
            // 项目目标落库
            projectGoalMapper.batchInsert(projectGoalDOs);
        }

        // 添加项目预算信息
        List<ProjectBudgetSaveReq> projectBudgets = projectInnerAddReq.getProjectBudgets();
        if (CollUtil.isNotEmpty(projectBudgets)) {
            // 转换
            List<ProjectBudgetDO> projectBudgetDOs = projectBudgets.stream()
                    .map(e -> ProjectBudgetsCopier.INSTANCE.req2do(e, projectId)).collect(Collectors.toList());

            for (ProjectBudgetDO projectBudgetDO : projectBudgetDOs) {
                // 落库
                projectBudgetMapper.insert(projectBudgetDO);
            }
        }

        // 记录项目状态日志
        Integer status = ProjectStatusEnum.WAITING.getCode();
        projectLogComponent.addLogWhenStatusChange(status, status, projectDO.getId(), ButtonActionEnum.SUBMIT.getText());

        applicationEventPublisher.publishEvent(new ProjectCreateEvent(this, projectDO));
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProjectModifyReq projectModifyReq) {
        log.info("项目修改接收参数:{}", projectModifyReq);
        ProjectDO oldProject = projectMapper.getByName(projectModifyReq.getName());
        if (oldProject != null && !oldProject.getId().equals(projectModifyReq.getId())) {
            throw new BaseBizRuntimeException("该项目名称已存在,请修改后重试");
        }
        if (oldProject == null) {
            oldProject = projectMapper.get(projectModifyReq.getId());
        }
        // 校验项目目标
        if (YesOrNoEnum.YES.getCode().equals(projectModifyReq.getIsWithGoal())) {
            AssertUtil.notEmpty(projectGoalMapper.getByProjectId(oldProject.getId()),
                    "项目含有项目目标，请至少添加一条项目目标数据");
        }
        ProjectDO newProject = ProjectCopier.INSTANCE.convert(projectModifyReq);
        List<ProjectNodeDO> projectNodeDOList = ProjectNodeCopier.INSTANCE.convert(projectModifyReq.getProjectNodes());

        if (projectModifyReq.getDelayType() >= 1) {
            //有流程,计划时间不能变
            ProjectDO oldProjectDO = projectMapper.get(projectModifyReq.getId());
            newProject.setPlanStartDate(oldProjectDO.getPlanStartDate());
            newProject.setPlanEndDate(oldProjectDO.getPlanEndDate());
        }

        fillInfoWhenModify(projectNodeDOList, newProject);

        taskComponent.containProductLineInTask(newProject.getId(), newProject.getProductLineIds());

        bugOfflineComponent.containProductLineInBugOffline(newProject.getId(), newProject.getProductLineIds());

        List<String> pdUserIds = projectModifyReq.getPds().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        // 团队成员
        List<PersonAddReq> teamMembers = projectModifyReq.getTeamMembers();
        //过滤掉重复选择的项目经理,产品经理
        teamMembers = teamMembers.stream().filter(a -> !a.getUserId().equals(newProject.getPmId()) && !pdUserIds.contains(a.getUserId()))
                .collect(Collectors.toList());
        teamMembers.addAll(projectModifyReq.getPds());
        if (!pdUserIds.contains(projectModifyReq.getPm().getUserId())) {
            //产品经理不包含项目经理时,将项目经理加入团队中
            teamMembers.add(projectModifyReq.getPm());
        }
        personComponent.update(teamMembers, newProject.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());

        // 节点信息
        if (CollectionUtils.isNotEmpty(projectNodeDOList)) {
            boolean match = projectNodeDOList.stream().anyMatch(e ->
                    ProjectNodeEnum.PUBLISH_OFFICIAL.getText().equals(e.getName()) && e.getActualDate() != null);
            if (match && !checkProductRelease(projectModifyReq.getId())) {
                throw new BaseBizRuntimeException("该项目还有bug未关闭，请关闭后再发布");
            }
            if (projectModifyReq.getDelayType() >= 1) {
                //需要审批,只更新实际时间
                projectNodeComponent.updateNodeActualDate(projectNodeDOList, newProject.getId());
            } else {
                projectNodeComponent.add(projectNodeDOList, newProject.getId());
            }
            // 更新节点状态
            projectComponent.updateNodeStatus(projectModifyReq.getId());
        }
        // log
        projectLogComponent.addLogWhenModifyData(oldProject, newProject);
        // 产品线
        projectProductLineComponent.update(newProject.getProductLineIds(), newProject.getId());
        // 产品经理
        personComponent.update(projectModifyReq.getPds(), newProject.getId(), PersonTypeEnum.PROJECT_PD.getCode());

        //立项时间变化
        sendDingMsgIfPublishDateForward(newProject.getId(), oldProject.getPjEstablishPublishDate());

        //流程与版本信息处理
        processFlow(projectModifyReq);

        // 人天如果更换项目经理
        if (!Objects.equals(oldProject.getPmId(), newProject.getPmId())) {
            manDayReportComponent.updateAuditor(newProject.getId());
            manDayReportComponent.batchMsg(newProject.getId());
        }
        innerProjectStatusUpdateComponent.updateFromProject(newProject);

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> simpleModify(ProjectSimpleModifyReq projectSimpleModifyReq) {
        // 校验名称参数
        Long projectId = projectSimpleModifyReq.getId();
        String projectName = projectSimpleModifyReq.getName();
        if (StrUtil.isNotBlank(projectName)) {
            AssertUtil.checkState(!projectName.contains(CommonConstant.BLANK), "项目名称中请勿包含空格");

            ProjectDO byName = projectMapper.getByName(projectName);
            AssertUtil.checkState(byName == null || Objects.equals(projectId, byName.getId()), "该项目名称已存在,请修改后重试");
        }

        // 目标项目id
        ProjectDO oldProjectDO = projectMapper.get(projectId);
        AssertUtil.notNull(oldProjectDO, "项目不存在");

        // 转换，更新落库
        ProjectDO updateProjectDO = ProjectCopier.INSTANCE.sreq2do(projectSimpleModifyReq);
        projectMapper.update(updateProjectDO);

        // 项目预算
        String expectedIncome = projectSimpleModifyReq.getExpectedIncome();
        if (expectedIncome != null) {
            if ("".equals(expectedIncome)) {
                expectedIncome = null;
            }
            projectMapper.updateExpectIncome(projectId, expectedIncome);
        }

        // 扩展成员
        List<PersonAddReq> extTeamMembers = projectSimpleModifyReq.getExtTeamMembers();
        if (extTeamMembers != null) {
            // 旧版成员
            List<PersonDO> oldMembers = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.EXTENSION.getCode());

            String addMembers = extTeamMembers.stream()
                    .map(PersonAddReq::getUserName)
                    .filter(e -> oldMembers.stream().noneMatch(x -> Objects.equals(e, x.getUserName())))
                    .collect(Collectors.joining(","));

            String deleteMembers = oldMembers.stream()
                    .map(PersonDO::getUserName)
                    .filter(e -> extTeamMembers.stream().noneMatch(x -> Objects.equals(e, x.getUserName())))
                    .collect(Collectors.joining(","));

            // 日志
            projectLogComponent.addNewProjectMemberLog(projectId, addMembers);
            projectLogComponent.addDeleteProjectMemberLog(projectId, deleteMembers);

            // 实际更新落库
            personComponent.update(extTeamMembers, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.EXTENSION.getCode());

            // 更新成员等级
            List<String> extMemberIdList = extTeamMembers.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
            personMapper.updateLevel(extMemberIdList, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.EXTENSION.getCode());
        }

        // 核心成员
        PersonAddReq pm = projectSimpleModifyReq.getPm();
        List<PersonAddReq> newMembers = projectSimpleModifyReq.getTeamMembers();
        if (newMembers != null || pm != null) {
            // 旧版成员
            List<PersonDO> oldMembers = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.CORE.getCode());

            // 成员更新日志
            if (newMembers == null) {
                newMembers = oldMembers.stream()
                        .map(PersonCopier.INSTANCE::convert)
                        .collect(Collectors.toList());
            } else if (pm == null){
                pm = new PersonAddReq();
                pm.setUserId(oldProjectDO.getPmId());
                pm.setUserName(oldProjectDO.getPm());
            }
            personComponent.duplicateRemove(newMembers, pm);

            List<PersonAddReq> finalNewMembers = newMembers;

            String addMembers = finalNewMembers.stream()
                    .map(PersonAddReq::getUserName)
                    .filter(e -> oldMembers.stream().noneMatch(x -> Objects.equals(e, x.getUserName())))
                    .collect(Collectors.joining(","));

            String deleteMembers = oldMembers.stream()
                    .map(PersonDO::getUserName)
                    .filter(e -> finalNewMembers.stream().noneMatch(x -> Objects.equals(e, x.getUserName())))
                    .collect(Collectors.joining(","));

            // 日志
            projectLogComponent.addNewProjectMemberLog(projectId, addMembers);
            projectLogComponent.addDeleteProjectMemberLog(projectId, deleteMembers);

            // 实际更新落库
            personComponent.update(finalNewMembers, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.CORE.getCode());

            // 更新成员等级
            List<String> coreMemberIdList = finalNewMembers.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
            personMapper.updateLevel(coreMemberIdList, projectId, PersonTypeEnum.PROJECT_MEMBER.getCode(), PersonLevelEnum.CORE.getCode());
        }

        // 项目日志
        ProjectDO newProjectDO = projectMapper.get(projectId);
        projectLogComponent.addLogWhenSimpleModifyData(oldProjectDO, newProjectDO);

        innerProjectStatusUpdateComponent.updateFromProject(newProjectDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> listChildren(ProjectChildListReq projectChildListReq) {
        ProjectDO project = projectMapper.get(projectChildListReq.getProjectId());
        AssertUtil.notNull(project, "您查询的项目不存在，请检查");
        ProjectListChildCondition condition = ProjectCopier.INSTANCE.convert(projectChildListReq, project);
        if (projectChildListReq.getNavigateProjectId() != null) {
            ProjectDO navigateProject = projectMapper.get(projectChildListReq.getNavigateProjectId());
            AssertUtil.notNull(navigateProject, "您选择的项目树节点不存在，请检查");
            condition.setNavigateParentIdsPrefix(navigateProject.getParentIds());
        }
        PageHelper.startPage(condition.getPageNum(), condition.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        Page<ProjectListDO> projects = projectMapper.listChildren(condition);
        List<ProjectVO> resultList = ProjectCopier.INSTANCE.convert(projects);
        int baseProjectDepth = project.getParentList().size();
        for (ProjectVO projectVO : resultList) {
            projectVO.setNodeDepth(projectVO.getNodeDepth() - baseProjectDepth + 1);
        }
        PageQueryResult<ProjectVO> res = PageQueryResult.resResult(resultList);
        ResultUtil.fillPageInfo(res, projects);
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<ProjectTreeVO> getTree(Long projectId) {
        AssertUtil.notNull(projectId, "请提供项目id");
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "项目不存在");
        List<ProjectDO> projects = projectMapper.selectByParentIdsRegexp("^" + project.getParentIds());
        List<ProjectTreeVO> treeList = ProjectCopier.INSTANCE.convertTree(projects);
        Map<Long, ProjectTreeVO> projectById = Maps.uniqueIndex(treeList, ProjectTreeVO::getProjectId);
        Map<Long, List<ProjectTreeVO>> treeByParentId = treeList.stream()
                .filter(t -> Objects.nonNull(t.getParentId()))
                .collect(Collectors.groupingBy(ProjectTreeVO::getParentId));
        for (Map.Entry<Long, List<ProjectTreeVO>> treeEntry : treeByParentId.entrySet()) {
            ProjectTreeVO parentProject = projectById.get(treeEntry.getKey());
            if (parentProject != null) {
                parentProject.setChildren(treeEntry.getValue());
            }
        }
        return BaseResult.success(projectById.get(projectId));
    }

    @Override
    public BaseResult<Void> appendChildren(ProjectAppendChildReq projectAppendChildReq) {
        Long projectId = projectAppendChildReq.getProjectId();
        List<Long> childIds = projectAppendChildReq.getChildIds();
        ProjectDO parentProject = projectMapper.get(projectId);
        AssertUtil.notNull(parentProject, "父项目不存在，请刷新后重试");
        List<ProjectDO> childProjects = projectMapper.getByIds(childIds);
        AssertUtil.notEmpty(childProjects, "子项目不存在，请刷新后重试");
        AssertUtil.checkState(childProjects.stream().map(ProjectDO::getParentId).allMatch(Objects::isNull),
                "存在子项目已有父项目，无法再次关联");
        Set<Long> childIdsSet = new HashSet<>(childIds);
        AssertUtil.checkState(parentProject.getParentList().stream().noneMatch(childIdsSet::contains),
                "存在子项目为当前项目父节点，无法关联");
        for (ProjectDO childProject : childProjects) {
            projectComponent.attachChildProject(parentProject, childProject);
        }
        return BaseResult.success();
    }

    @Override
    public BaseResult<Void> deleteChild(ProjectDeleteChildReq projectDeleteChildReq) {
        ProjectDO parentProject = projectMapper.get(projectDeleteChildReq.getProjectId());
        AssertUtil.notNull(parentProject, "父项目不存在");
        ProjectDO childProject = projectMapper.get(projectDeleteChildReq.getChildId());
        AssertUtil.notNull(childProject, "子项目不存在");
        projectComponent.deleteChildProject(parentProject, childProject);
        return BaseResult.success();
    }

    @Override
    public BaseResult<ProjectTabCountVO> countTabTodos(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 项目风险个数
        Long riskCount = projectRiskMapper.count(projectId, ProjectRiskStatusEnum.PENDING.getCode());

        // 子项目个数，查询包含自身，需要减一
        Long childrenCount = projectMapper.countChildren(Collections.singleton(projectId))
                .stream()
                .map(ProjectChildCountDO::getChildCount)
                .findFirst().orElse(0L);

        // 返回数据
        ProjectTabCountVO tabCountVO = new ProjectTabCountVO();
        tabCountVO.setProjectRiskCount(riskCount);
        tabCountVO.setChildrenCount(childrenCount);
        return BaseResult.success(tabCountVO);
    }

    @Override
    public BaseResult<ProjectDetailVO> get(Long projectId) {
        log.info("项目查看接收参数:{}", projectId);
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("该项目不存在");
        }
        ProjectDetailVO projectDetailVO = ProjectCopier.INSTANCE.convert(projectDO);
        projectDetailVO.setStatusName(ProjectStatusEnum.getTextByCode(projectDetailVO.getStatus()));
        projectDetailVO.setPriorityName(PriorityEnum.getTextByCode(projectDetailVO.getPriority()));
        projectDetailVO.setTypeName(ProjectTypeEnum.getTextByCode(projectDetailVO.getType()));
        projectDetailVO.setLevelName(ProjectLevelEnum.getTextByCode(projectDetailVO.getLevel()));

        //产品线
        List<ProductLineDO> productLineDO = productLineMapper.get(projectId);
        List<ProductLineVO> productLineVO = ProductLineCopier.INSTANCE.convert(productLineDO);
        projectDetailVO.setProductLineVO(productLineVO);

        // 产品经理
        List<PersonDO> pds = personComponent.select(projectId, PersonTypeEnum.PROJECT_PD.getCode());
        projectDetailVO.setPd(PersonCopier.INSTANCE.transform(pds));
        List<String> pdUserIds = pds.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        // 团队成员
        List<PersonDO> teamMembers = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        //过滤掉产品经理和项目经理
        teamMembers = teamMembers.stream().filter(a -> !a.getUserId().equals(projectDO.getPmId()) && !pdUserIds.contains(a.getUserId()))
                .collect(Collectors.toList());
        projectDetailVO.setTeamMember(PersonCopier.INSTANCE.transform(teamMembers));

        //节点
        List<ProjectNodeDO> projectNodeDO = projectNodeComponent.get(projectId);
        List<ProjectNodeVO> projectNodeVO = ProjectNodeCopier.INSTANCE.transform(projectNodeDO);
        projectDetailVO.setProjectNodes(projectNodeVO);
        Date currentDate = new Date();
        projectDetailVO.setCurrentDate(currentDate);

        // 节点状态
        projectDetailVO.setNodeStatusName(ProjectNodeStatusEnum.getNameByCode(projectDetailVO.getNodeStatus()));
        //详设
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectIdAndType(projectId, ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getCode());
        if (CollectionUtils.isNotEmpty(projectFlowDos)) {
            projectFlowDos.sort(Comparator.comparing(ProjectFlowDO::getCreateDate).reversed());
            ProjectFlowDO oldFlowDo = projectFlowDos.get(0);
            projectDetailVO.setProjectFlowId(oldFlowDo.getId());
        }
        if (projectDO.getStatus() < 0) {
            // 项目已暂停或者作废，则拿暂停、作废时间作为完成时间
            List<BizChangeLogDO> logs = bizChangeLogMapper.listAllByActions(Collections.singletonList(projectDO.getId()),
                    BizChangeLogTypeEnum.PROJECT.getCode(),
                    Lists.newArrayList(ButtonActionEnum.SUSPEND.getText(), ButtonActionEnum.INVALID.getText()));
            // 最新一次暂停或者作废记录的时间
            logs.stream().map(BizChangeLogDO::getCreateDate)
                    .max(Date::compareTo).ifPresent(projectDetailVO::setSuspendDate);
        }

        BigDecimal resourceAssessment = projectDetailVO.getResourceAssessment();
        if (resourceAssessment != null) {
            projectDetailVO.setResourceAssessment(resourceAssessment.setScale(2, RoundingMode.DOWN));
        }
        //发布正式
        List<ProjectNodeFlowDO> projectNodeFlows = projectNodeFlowMapper.getByProjectId(projectId);
        if (CollectionUtils.isNotEmpty(projectNodeFlows)) {
            ProjectNodeFlowDO oldFlowDo = projectNodeFlows.get(0);
            projectDetailVO.setPublishFlowId(oldFlowDo.getId());
            projectDetailVO.setPublishFlowStatus(oldFlowDo.getStatus());
            long count = projectNodeFlows.stream().filter(a -> FlowStatusEnum.COMPLETE.getCode().equals(a.getStatus())).count();
            projectDetailVO.setPublishChangeCount(count);
        }
        projectDetailVO.setIsPMO(userComponent.isPmoOrPmoLeader());
        return BaseResult.success(projectDetailVO);
    }

    @Override
    public BaseResult<ProjectInnerDetailVO> getInner(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "该项目不存在");

        // 转换
        ProjectInnerDetailVO projectInnerDetailVO = ProjectCopier.INSTANCE.do2Vo(projectDO);

        // 团队成员
        List<PersonDO> teamMemberDOList = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());

        List<PersonDO> coreTeamMemberDOList = teamMemberDOList.stream()
                .filter(e -> PersonLevelEnum.CORE.getCode().equals(e.getPersonLevel()))
                .collect(Collectors.toList());
        List<PersonDO> extTeamMemberDOList = teamMemberDOList.stream()
                .filter(e -> PersonLevelEnum.EXTENSION.getCode().equals(e.getPersonLevel()))
                .collect(Collectors.toList());

        List<PersonVO> coreTeamMemberVOList = PersonCopier.INSTANCE.transform(coreTeamMemberDOList);
        List<PersonVO> extTeamMemberVOList = PersonCopier.INSTANCE.transform(extTeamMemberDOList);

        projectInnerDetailVO.setTeamMember(coreTeamMemberVOList);
        projectInnerDetailVO.setExtTeamMembers(extTeamMemberVOList);

        // 上级项目信息
        Long parentId = projectDO.getParentId();
        if (parentId != null) {
            ProjectDO parentProjectDO = projectMapper.get(parentId);
            projectInnerDetailVO.setParentId(parentProjectDO.getId());
            projectInnerDetailVO.setParentProjectName(parentProjectDO.getName());
        }

        // 是否为项目经理和PMO及其上级
        String pmId = projectDO.getPmId();
        boolean isLeaderOrPMO = userComponent.isPmoOrPmoLeader() || isLeader(pmId);
        projectInnerDetailVO.setIsLeaderOrPMO(isLeaderOrPMO);

        return BaseResult.success(projectInnerDetailVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(ProjectLinkProductDemandQueryList query) {
        log.info("项目-产品需求匹配,接收参数:{}", query);
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(query);
        // 过滤掉已经关联的产品需求
        List<Long> productDemandIds = projectProductDemandMapper.getLinkedProductDemand(Lists.newArrayList())
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        condition.setFilterProductDemandIds(productDemandIds);
        condition.setStatus(Lists.newArrayList(ProductDemandStatusEnum.WAITING.getCode()
                , ProductDemandStatusEnum.INCLUDED.getCode()
                , ProductDemandStatusEnum.PROGRESS.getCode()
                , ProductDemandStatusEnum.ONLINE.getCode()));

        //是否打标
        List<BizLabelDO> bizLabelDOList;
        if (CollectionUtils.isNotEmpty(query.getLabelIds()) || CollectionUtils.isNotEmpty(query.getLabelCategoryIds())) {
            List<Long> newLabelIds = labelComponent.getLabelIds(query.getLabelIds(), query.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(newLabelIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(bizIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setInProductDemandIds(bizIds);
        }
        PageHelper.startPage(query.getPageNum(), query.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandComponent.list(condition);
        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandListDO);

        if (CollectionUtils.isEmpty(productDemandListDO)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        List<Long> pids = productDemandListDO.stream().map(ProductDemandListDO::getId).collect(Collectors.toList());

        Map<Long, List<BizLabelSimpleVO>> bizLabelMap = bizLabelComponent.getBizLabelMap(pids, BizTypeEnum.PRODUCT_DEMAND.getCode());

        for (ProductDemandVO a : productDemandVOList) {
            a.setStatusName(ProductDemandStatusEnum.getTextByCode(a.getStatus()));
            a.setPriorityName(PriorityEnum.getTextByCode(a.getPriority()));

            List<BizLabelSimpleVO> labelSimpleVOList = bizLabelMap.get(a.getId());
            if (CollectionUtils.isNotEmpty(labelSimpleVOList)) {
                a.setLabelNames(labelSimpleVOList);
            }
        }
        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<ProductDemandStatusVO> linkOrUnLinkProductDemand(ProjectProductDemandLinkReq productDemandLinkReq) {
        log.info("关联or取消关联接收参数:{}", productDemandLinkReq);
        ProjectDO projectDO = projectMapper.get(productDemandLinkReq.getProjectId());
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        List<Long> productDemandIds = productDemandLinkReq.getProductDemandIds();
        List<ProductDemandDO> productDemands = productDemandMapper.selectByIdList(productDemandIds);
        Map<Long, String> pdNameMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (v1, v2) -> v2));
        Map<Long, Integer> statusMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getStatus, (v1, v2) -> v2));

        if (LinkOrUnLinkEnum.LINK.getCode().equals(productDemandLinkReq.getType())) {
            List<ProjectProductDemandDO> productDemand = projectProductDemandMapper.getLinkedProductDemand(productDemandIds);
            if (CollectionUtils.isNotEmpty(productDemand)) {
                List<Long> existedIds = productDemand.stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
                throw new BaseBizRuntimeException("产品需求id为" + existedIds + "已被项目关联,请刷后重试");
            }
            List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByProductDemandIds(productDemandIds);
            Map<Long, Integer> bizIdMap = productBizDemandDOList.stream().collect(Collectors.toMap(ProductBizDemandDO::getBizDemandId, ProductBizDemandDO::getStatus, (v1, v2) -> v2));

            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus(), productDemandIds);

            projectProductDemandComponent.batchInsert(projectDO.getId(), productDemandIds);

            //项目关联后,业务需求的发布时间可能变化
            bizIdMap.forEach((k, v) -> {
                BizDemandDO bizDemandDO = bizDemandMapper.selectById(k);
                productDemandComponent.sendDingMsg(v, bizDemandDO.getStatus(), k);
            });

            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, ButtonActionEnum.LINK.getText());

        } else {
            ProductDemandDO productDemandDO = new ProductDemandDO();
            productDemandDO.setId(productDemandIds.get(0));
            productDemandDO.setStatus(ProductDemandStatusEnum.WAITING.getCode());
            productDemandComponent.update(productDemandDO);

            projectProductDemandComponent.update(null, productDemandIds.get(0));

            // 一个产品需求下的业务需求
            productDemandComponent.updateDemandStatusAsProductStatusChange(productDemandIds, false);

            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, ButtonActionEnum.UN_LINK.getText());
            productDemandLogComponent.addLogAsProjectStatusChange(statusMap, productDemandDO.getStatus());

            // 取消产品需求和任务的关联
            productDemandIds.forEach(a -> taskProductDemandComponent.update(null, a));
        }
        //产品需求和项目关联或删除时,需要给前端刷新产品需求状态
        ProductDemandDO productDemandDO = productDemandMapper.selectById(productDemandIds.get(0));
        ProductDemandStatusVO vo = new ProductDemandStatusVO();
        vo.setStatus(productDemandDO.getStatus());
        vo.setStatusText(ProductDemandStatusEnum.getTextByCode(productDemandDO.getStatus()));
        return BaseResult.success(vo);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> linkProductDemandList(ProjectProductDemandQueryList query) {
        Long projectId = query.getProjectId();
        int pageSize = query.getPageSize();
        int pageNum = query.getPageNum();
        log.info("项目-产品需求清单:{},{},{}", pageNum, pageSize, projectId);
        // 查询产品需求
        List<ProductDemandListDO> productDemandListDO = productDemandMapper.linkProductDemandList(projectId);
        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandListDO);

        if (CollectionUtils.isNotEmpty(productDemandVOList)) {
            //查询产品需求关联任务
            List<Long> productDemandIdList = productDemandVOList.stream().map(ProductDemandVO::getId).collect(Collectors.toList());
            List<TaskProductDemandDO> taskProductDemandDOList = taskProductDemandMapper.selectByProductDemandId(productDemandIdList);

            // 根据产品id分类
            Map<Long, List<TaskProductDemandDO>> taskProductDemandMap =
                    taskProductDemandDOList.stream().collect(Collectors.groupingBy(TaskProductDemandDO::getProductDemandId));

            // 填充任务数
            for (ProductDemandVO e : productDemandVOList) {
                int taskCount = taskProductDemandMap.containsKey(e.getId()) ? taskProductDemandMap.get(e.getId()).size() : 0;
                e.setTaskCount(taskCount);

                e.setProjectId(projectId);
                e.setStatusName(ProductDemandStatusEnum.getTextByCode(e.getStatus()));
                e.setPriorityName(PriorityEnum.getTextByCode(e.getPriority()));
            }
        }

        if (Integer.valueOf(0).equals(query.getType())) {
            productDemandVOList = productDemandVOList.stream().filter(a -> a.getTaskCount() == 0).collect(Collectors.toList());
        } else if (Integer.valueOf(1).equals(query.getType())) {
            productDemandVOList = productDemandVOList.stream().filter(a -> a.getTaskCount() >= 1).collect(Collectors.toList());
        }

        int count = productDemandVOList.size();
        if (count > pageSize) {
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, count);
            productDemandVOList = productDemandVOList.subList(fromIndex, toIndex);
        }

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVOList);
        pageQueryResult.setTotalItems(count);
        pageQueryResult.setTotalPages(count % pageSize == 0 ? count / pageSize : (count / pageSize) + 1);
        pageQueryResult.setCurrentPage(pageNum);
        pageQueryResult.setItemsPerPage(pageSize);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<ProjectBaseVO>> getProjectByProductLine(Long productLineId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<ProjectDO> projectDOList = projectMapper.selectByProductLine(productLineId, userInfo.getId());
        List<ProjectBaseVO> projectBaseVOList = projectDOList.stream().map(ProjectCopier.INSTANCE::convertTo).collect(Collectors.toList());

        return BaseResult.success(projectBaseVOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modifyProjectDate(ProjectDateModifyReq projectDateModifyReq) {
        log.info("立项时间修改参数:{}", projectDateModifyReq);
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectDateModifyReq);
        ProjectDO oldProjectDO = projectMapper.get(projectDO.getId());

        checkPjEstablishPublishDateChange(oldProjectDO, projectDateModifyReq.getPjEstablishPublishDate());

        Date pjEstablishStartDate = projectDateModifyReq.getPjEstablishStartDate();
        Date pjEstablishPublishDate = projectDateModifyReq.getPjEstablishPublishDate();


        String oldStartDateStr = DateUtil.parseToString(oldProjectDO.getPjEstablishStartDate(), DateStyle.YYYY_MM_DD);
        String newStartDateStr = DateUtil.parseToString(pjEstablishStartDate, DateStyle.YYYY_MM_DD);
        projectLogComponent.addLogWhenContentChange(oldStartDateStr, newStartDateStr, projectDO.getId(), BizChangeLogFieldEnum.PJ_ESTABLISH_START_DATE.getText());

        Date oldPublishDate = oldProjectDO.getPjEstablishPublishDate();
        String oldPublishDateStr = DateUtil.parseToString(oldPublishDate, DateStyle.YYYY_MM_DD);
        String newPublishDateStr = DateUtil.parseToString(pjEstablishPublishDate, DateStyle.YYYY_MM_DD);
        projectLogComponent.addLogWhenContentChange(oldPublishDateStr, newPublishDateStr, projectDO.getId(), BizChangeLogFieldEnum.PJ_ESTABLISH_PUBLISH_DATE.getText());

        oldProjectDO.setPjEstablishStartDate(pjEstablishStartDate);
        oldProjectDO.setPjEstablishPublishDate(pjEstablishPublishDate);

        projectMapper.fullUpdateById(oldProjectDO);
        sendDingMsgIfPublishDateForward(projectDO.getId(), oldPublishDate);
        innerProjectStatusUpdateComponent.updateFromProject(projectDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Void> addStage(ProjectStageChangeReq addStageReq) {
        Long projectId = addStageReq.getProjectId();
        Integer stage = addStageReq.getStage();
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "项目不存在");
        AssertUtil.checkState(Objects.equals(project.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode()),
                "只有内部项目才能添加阶段");
        AssertUtil.checkState(Objects.equals(ProjectStageEnum.OPERATE.getCode(), stage), "目前仅支持添加运营阶段");
        List<Integer> stageList = project.getValidStageList();
        if (stageList.contains(stage)) {
            return BaseResult.success();
        }
        stageList.add(stage);
        ProjectDO updateCond = new ProjectDO();
        updateCond.setId(projectId);
        updateCond.setValidStageList(stageList);
        projectMapper.update(updateCond);
        // 添加阶段需要同时更新自身和父级项目的时间
        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(project.getId());
        innerProjectStatusUpdateComponent.updateFromProject(project);
        return BaseResult.success();
    }

    @Override
    public BaseResult<Void> deleteStage(ProjectStageChangeReq deleteStageReq) {
        Long projectId = deleteStageReq.getProjectId();
        Integer stage = deleteStageReq.getStage();
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "项目不存在");
        AssertUtil.checkState(Objects.equals(project.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode()),
                "只有内部项目才能删除阶段");
        AssertUtil.checkState(Objects.equals(ProjectStageEnum.OPERATE.getCode(), stage), "目前仅支持删除运营阶段");
        List<Integer> stageList = project.getValidStageList();
        if (!stageList.contains(stage)) {
            return BaseResult.success();
        }
        // 删除阶段，同时删除阶段下的里程碑
        projectMilestoneMapper.selectByProjectId(projectId)
                .stream().filter(m -> Objects.equals(m.getStage(), stage))
                .forEach(m -> projectMilestoneService.deleteMilestone(m.getId()));
        stageList.remove(stage);
        ProjectDO updateCond = new ProjectDO();
        updateCond.setId(projectId);
        updateCond.setValidStageList(stageList);
        projectMapper.update(updateCond);
        // 删除阶段需要同时更新自身和父级项目的时间
        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(project.getId());
        innerProjectStatusUpdateComponent.updateFromProject(project);
        return BaseResult.success();
    }

    @Override
    public BaseResult<List<ProjectProductLineVO>> getByName(String name) {
        String likeName = StringUtil.toLikeStr(name);
        List<ProjectDO> projectDOList = projectMapper.getByLikeName(likeName, ProjectCategoryEnum.PRODUCT_PROJECT.getCode());
        projectDOList = projectDOList.stream()
                .filter(a -> !ProjectStatusEnum.INVALID.getCode().equals(a.getStatus())
                        && !ProjectStatusEnum.RELEASED.getCode().equals(a.getStatus()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(projectDOList)) {
            return BaseResult.success(Lists.emptyList());
        }
        List<Long> projectIdList = projectDOList.stream().map(ProjectDO::getId).collect(Collectors.toList());
        List<ProjectProductLineDO> ppLines = projectProductLineMapper.getByProjectIdList(projectIdList);
        if (CollectionUtils.isEmpty(ppLines)) {
            return BaseResult.success(Lists.emptyList());
        }
        Map<Long, Set<Long>> ppIdMap = ppLines.stream()
                .collect(Collectors.groupingBy(ProjectProductLineDO::getProjectId, Collectors.mapping(ProjectProductLineDO::getProductLineId, Collectors.toSet())));

        List<Long> productLineIds = ppLines.stream().map(ProjectProductLineDO::getProductLineId).collect(Collectors.toList());
        List<ProductLineDO> productLineDOList = productLineMapper.selectByIds(productLineIds);
        Map<Long, ProductLineDO> pdlMap = productLineDOList.stream().collect(Collectors.toMap(ProductLineDO::getId, a -> a, (v1, v2) -> v2));

        List<ProjectProductLineVO> result = projectDOList.stream().map(a -> {
            ProjectProductLineVO o = new ProjectProductLineVO();
            o.setId(a.getId());
            o.setName(a.getName());
            List<ProductLineDO> pdls = ppIdMap.get(a.getId()).stream().filter(pdlMap::containsKey).map(pdlMap::get).collect(Collectors.toList());
            o.setProductLines(ProductLineCopier.INSTANCE.convert(pdls));
            return o;
        }).collect(Collectors.toList());
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Boolean> modifyUnWriteReason(ProjectUnWriteReasonModifyReq reasonModifyReq) {
        ProjectDO update = new ProjectDO();
        update.setId(reasonModifyReq.getId());
        update.setUnWriteReason(reasonModifyReq.getUnWriteReason());
        projectMapper.update(update);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> innerComplete(ProjectInnerCompleteReq req) {
        Long projectId = req.getProjectId();
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");
        AssertUtil.checkState(Objects.equals(projectDO.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode()),
                "该项目不是内部项目");
        AssertUtil.checkState(!Objects.equals(projectDO.getStatus(), ProjectStatusEnum.SUSPEND.getCode())
                        && !Objects.equals(projectDO.getStatus(), ProjectStatusEnum.INVALID.getCode()),
                "项目暂停或作废时，不能进行此操作");

        // 里程碑关联的任务与项目,过滤作废里程碑
        List<ProjectMilestoneVO> projectMilestones = projectMilestoneComponent.listByProjectId(projectId);
        projectMilestones = projectMilestones.stream().filter(e -> {
            if (MilestoneTypeEnum.TASK.getCode().equals(e.getType())) {
                return !TaskStatusEnum.INVALID.getCode().equals(e.getStatus());
            } else {
                return !ProjectStatusEnum.INVALID.getCode().equals(e.getStatus());
            }
        }).collect(Collectors.toList());

        // 最后一个阶段存在里程碑
        List<Integer> validStages = projectDO.getValidStageList();
        Integer completeStage = validStages.get(validStages.size() - 1);
        Set<Integer> milestoneStages = projectMilestones.stream().map(ProjectMilestoneVO::getStage)
                .collect(Collectors.toSet());
        AssertUtil.checkState(milestoneStages.contains(completeStage),
                ProjectStageEnum.getByCode(completeStage).getText() + "无里程碑，无法完成项目");
        Date projectActualEndDate = projectMilestones.stream()
                .filter(m -> Objects.equals(m.getStage(), completeStage))
                .map(ProjectMilestoneVO::getActualEndDate)
                .max(Date::compareTo)
                .orElse(new Date());
        // 里程碑是否全部完成
        AssertUtil.checkState(projectMilestones.stream().noneMatch(e -> Objects.isNull(e.getActualEndDate())),
                "存在未完成的里程碑，无法关闭项目");

        // 修改项目状态
        projectMapper.updateStatus(projectId, ProjectStatusEnum.COMPLETE.getCode());
        projectMapper.updateStatusAndEndDate(projectId, ProjectStatusEnum.COMPLETE.getCode(), projectActualEndDate);

        // 项目状态日志
        projectLogComponent.addLogWhenStatusChange(projectDO.getStatus(), ProjectStatusEnum.COMPLETE.getCode(),
                projectId, ButtonActionEnum.INNER_FINISH.getText());

        innerProjectStatusUpdateComponent.updateFromProject(projectDO);
        return BaseResult.success();
    }

    @Override
    public BaseResult<PageQueryResult<ProjectSimpleVO>> pageAll(ProjectPageQuery query) {
        PageHelper.startPage(query.pageNum, query.pageSize);
        List<ProjectDO> doList = projectMapper.getByLikeName(query.getName(), null);

        List<ProjectSimpleVO> simpleVOList = ProjectCopier.INSTANCE.do2svo(doList);

        // 分页数据
        PageQueryResult<ProjectSimpleVO> result = new PageQueryResult<>();
        PageInfo<ProjectDO> pageInfo = new PageInfo<>(doList);
        result.setResultList(simpleVOList);
        ResultUtil.fillPageInfo(result, pageInfo);
        return BaseResult.success(result);
    }


    private boolean checkProductRelease(Long projectId) {
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

            bugLogMapper.batchInsert(bugLogDOList);
            bugOfflineMapper.unlinkBugOffline(postponeList);
        }

        return true;
    }

    private void fillInfoWhenModify(List<ProjectNodeDO> projectNodes, ProjectDO newProject) {
        ProjectDO oldProject = projectMapper.get(newProject.getId());

        checkBeforeUpdate(projectNodes, oldProject);

        checkPjEstablishPublishDateChange(oldProject, newProject.getPjEstablishPublishDate());

        checkAcceptBeforeUpdate(projectNodes, newProject);

        checkNodeDateBeforeUpdate(projectNodes, newProject.getId());

        Integer oldStatus = oldProject.getStatus();

        projectComponent.fillInfo(projectNodes, newProject);

        fieldUpdate(newProject, projectNodes, oldProject);

        projectMapper.fullUpdateById(newProject);

        if (!Objects.equals(newProject.getStatus(), oldStatus)) {
            //状态不一致时,更新产品需求状态
            productDemandComponent.updateProductDemandStatus(newProject.getId(), newProject.getStatus());
            projectLogComponent.addLogWhenStatusChange(oldStatus, newProject.getStatus(), newProject.getId(), ButtonActionEnum.MODIFY.getText());
        }
        if (!Objects.equals(oldProject.getPlanEndDate(), newProject.getPlanEndDate())
                || !Objects.equals(oldProject.getActualEndDate(), newProject.getActualEndDate())) {
            List<Long> productDemandIds = projectComponent.getLinkProductDemandIds(oldProject.getId());

            List<Long> bizDemandIds = productDemandComponent.getLinkBizDemandIds(productDemandIds);
            bizDemandIds.forEach(a -> bizDemandComponent.updateProjectEndDate(a));

            List<Long> customDemandIds = productDemandComponent.getLinkCustomDemandIds(productDemandIds);
            customDemandIds.forEach(a -> customDemandComponent.updateProjectEndDate(a));
        }
        log.info("更新项目信息完成");
    }

    private void checkBeforeUpdate(List<ProjectNodeDO> projectNodes, ProjectDO oldProjectDO) {
        Map<String, ProjectNodeDO> nodeMap = projectNodes
                .stream()
                .collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        // 检查任务
        boolean checkTask = nodeMap.get(ProjectNodeEnum.START_PLAN.getText()) == null
                && nodeMap.get(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText()) == null
                && nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE.getText()) == null
                && nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText()) == null
                && nodeMap.get(ProjectNodeEnum.UED_AUDIT.getText()) == null;
        if (checkTask) {
            //删除需求规划阶段时需要校验是否有关联任务,若有关联待执行&进行中&已完成&已暂停的任务,不能删除
            List<TaskDO> taskDOList = taskMapper.getByProjectId(oldProjectDO.getId())
                    .stream().filter(a -> ProjectStageEnum.DEMAND.getCode().equals(a.getStage())
                            && !TaskStatusEnum.INVALID.getCode().equals(a.getStatus())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(taskDOList)) {
                throw new BaseBizRuntimeException("需求规划阶段已关联任务，不可删除");
            }
        }
        // 计算项目状态
        ProjectNodeDO node;
        Integer oldStatus = oldProjectDO.getStatus();
        if ((node = nodeMap.get(ProjectNodeEnum.PUBLISH_OFFICIAL.getText())) != null && node.getActualDate() != null) {
            if (ProjectStatusEnum.SUSPEND.getCode().equals(oldStatus)) {
                // 编辑项目
                throw new BaseBizRuntimeException("项目状态为暂停时,不能填写发布正式的实际时间");
            }
            List<Date> nullDate = projectNodes.stream().map(ProjectNodeDO::getActualDate)
                    .filter(Objects::isNull).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(nullDate)) {
                throw new BaseBizRuntimeException("请填写完其他节点的实际时间后,再填写发布正式的实际时间");
            }
        }

        if (ProjectStatusEnum.terminated(oldStatus)) {
            throw new BaseBizRuntimeException("项目处于发布或作废中，不可编辑，请刷新后重试");
        }

    }

    private void checkPjEstablishPublishDateChange(ProjectDO oldProjectDO, Date pjEstablishPublishDate) {
        if (!Objects.equals(oldProjectDO.getPjEstablishPublishDate(), pjEstablishPublishDate)) {
            List<ProjectNodeFlowDO> projectNodeFlowDos = projectNodeFlowMapper.getByProjectId(oldProjectDO.getId());
            boolean match = projectNodeFlowDos.stream().anyMatch(a -> FlowStatusEnum.AUDITING.getCode().equals(a.getStatus()));
            if (match) {
                throw new BaseBizRuntimeException("发布正式节点流程处于审核中,不能修改立项预期上线时间");
            }
        }
    }

    private void checkAcceptBeforeUpdate(List<ProjectNodeDO> projectNodes, ProjectDO newProject) {
        if (YesOrNoEnum.NO.getCode().equals(newProject.getIsAcceptance())) {
            List<Integer> status = Lists.newArrayList(FlowStatusEnum.AUDITING.getCode(), FlowStatusEnum.COMPLETE.getCode(), FlowStatusEnum.REJECT.getCode());
            ProjectAcceptanceListCondition c = ProjectAcceptanceListCondition.builder().status(status).projectId(newProject.getId()).build();
            List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);
            if (CollectionUtils.isNotEmpty(list)) {
                throw new BaseBizRuntimeException("存在验收流程,不能将项目验收改为否");
            }
        }

        boolean released = projectNodes.stream().anyMatch(a ->
                ProjectNodeEnum.PUBLISH_OFFICIAL.getText().equals(a.getName()) && a.getActualDate() != null);
        if (released && YesOrNoEnum.YES.getCode().equals(newProject.getIsAcceptance())) {
            ProjectAcceptanceListCondition c = ProjectAcceptanceListCondition.builder().projectId(newProject.getId()).build();
            List<ProjectAcceptanceDO> list = projectAcceptanceMapper.list(c);
            boolean allWithdraw = list.stream().allMatch(a -> FlowStatusEnum.WITHDRAW.getCode().equals(a.getStatus()));
            if (CollectionUtils.isEmpty(list) || allWithdraw) {
                throw new BaseBizRuntimeException("您还没有发起项目验收,请验收通过后再发布");
            }
            list = list.stream().filter(a -> !FlowStatusEnum.WITHDRAW.getCode().equals(a.getStatus())).collect(Collectors.toList());
            Map<String, List<ProjectAcceptanceDO>> groupMap = list.stream().collect(Collectors.groupingBy(ProjectAcceptanceDO::getAcceptorId));
            groupMap.forEach((k, v) -> {
                List<ProjectAcceptanceDO> order = v.stream().sorted(Comparator.comparing(ProjectAcceptanceDO::getCreateDate).reversed()).collect(Collectors.toList());
                ProjectAcceptanceDO last = order.get(0);
                //去除已撤回的验收,最新一条不是已通过 不能发布
                if (FlowStatusEnum.AUDITING.getCode().equals(last.getStatus()) || FlowStatusEnum.REJECT.getCode().equals(last.getStatus())) {
                    throw new BaseBizRuntimeException("请确保所有验收人员验收通过后再发布");
                }
            });
        }
        if (released && Integer.valueOf(1).equals(newProject.getIsPlatformPublish())) {
            if (!projectPublishPlanComponent.linkPublishPlan(newProject.getId())) {
                throw new BaseBizRuntimeException("请关联发布计划");
            }
            if (projectPublishPlanComponent.anyMatchNotFinished(newProject.getId())) {
                throw new BaseBizRuntimeException("您的发布计划还未结束，请前往发布平台处理");
            }
        }
    }

    private void checkNodeDateBeforeUpdate(List<ProjectNodeDO> newProjectNodes, Long id) {
        List<ProjectNodeDO> oldProjectNodes = projectNodeMapper.get(id);
        Map<String, ProjectNodeDO> projectNodeMap = oldProjectNodes.stream().collect(Collectors.toMap(ProjectNodeDO::getName, a -> a, (v1, v2) -> v1));
        //找出可以发起审批的节点
        List<ProjectNodeDO> startFlowNodes = newProjectNodes.stream().filter(a -> ProjectNodeEnum.canStartFlow(a.getName())).collect(Collectors.toList());
        List<ProjectFlowDO> projectFlowDOList = projectFlowMapper.getByProjectId(id);
        List<Integer> flowTypes = projectFlowDOList.stream().filter(a -> !FlowStatusEnum.PRE_EDIT.getCode().equals(a.getStatus()))
                .map(ProjectFlowDO::getFlowType).collect(Collectors.toList());
        startFlowNodes.forEach(a -> {
            //有流程,实际时间不能修改
            if (flowTypes.contains(ProjectNodeEnum.getCodeByName(a.getName())) && projectNodeMap.containsKey(a.getName())
                    && !Objects.equals(projectNodeMap.get(a.getName()).getActualDate(), a.getActualDate())) {
                throw new BaseBizRuntimeException(String.format("%s节点存在审批流程,不能修改实际时间,请刷新后重试", a.getName()));
            }
        });

        Optional<ProjectNodeDO> optional = newProjectNodes.stream().filter(a -> ProjectNodeEnum.SUBMIT_TEST.getText().equals(a.getName())).findFirst();
        //提测节点
        if (optional.isPresent()) {
            ProjectNodeDO submitTest = optional.get();
            ProjectNodeDO oldSubmitTest = projectNodeMapper.getByName(id, ProjectNodeEnum.SUBMIT_TEST.getText());
            TestBillDO oldTestBillDO = testBillMapper.selectByProjectId(id);
            if (submitTest.getActualDate() == null && oldSubmitTest != null && oldSubmitTest.getActualDate() != null && oldTestBillDO != null) {
                throw new BaseBizRuntimeException("提测后,不能修改提测节点的实际时间,请刷新后重试");
            }

            if (oldTestBillDO != null && TestBillStatusEnum.TEST_SUCCESS.getCode().equals(oldTestBillDO.getStatus())
                    && oldSubmitTest != null && !Objects.equals(submitTest.getPlanDate(), oldSubmitTest.getPlanDate())) {
                //提测已经通过,修改计划时间,重算逾期时长
                TestBillDO testBillDO = new TestBillDO();
                if (submitTest.getActualDate().after(submitTest.getPlanDate())) {
                    String planDate = DateUtil.parseToString(submitTest.getPlanDate(), DateFormatConst.DATE_FORMAT);
                    String actualDate = DateUtil.parseToString(submitTest.getActualDate(), DateFormatConst.DATE_FORMAT);
                    testBillDO.setDelayDay(DateUtil.getIntervalDays(planDate, actualDate));
                } else {
                    testBillDO.setDelayDay(0);
                }
                testBillDO.setProjectId(id);
                testBillMapper.updateDelayDay(testBillDO, false);
            }
        }
    }

    private void fieldUpdate(ProjectDO newProject, List<ProjectNodeDO> projectNodes, ProjectDO oldProject) {
        newProject.setNodeStatus(oldProject.getNodeStatus());
        newProject.setUnWriteReason(oldProject.getUnWriteReason());
        if (ProjectStatusEnum.SUSPEND.getCode().equals(oldProject.getStatus())) {
            // 编辑项目时，当状态是暂停,不修改项目状态
            newProject.setStatus(oldProject.getStatus());
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(newProject.getStatus())) {
            List<String> needFillIn = projectDocumentComponent.docNeedFillIn(newProject.getId(), projectNodes, newProject.getType());
            if (CollectionUtils.isEmpty(needFillIn)) {
                //文档都已填写,清空未填写原因
                newProject.setUnWriteReason(null);
            }
        }
    }

    private void fillInfoWhenEnable(List<ProjectNodeDO> projectNodes, ProjectDO projectDO) {
        projectComponent.fillInfo(projectNodes, projectDO);
        projectMapper.fullUpdateById(projectDO);
        productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus());
    }

    private void processFlow(ProjectModifyReq projectModifyReq) {
        List<ProjectNodeDO> projectNodeDOList = ProjectNodeCopier.INSTANCE.convert(projectModifyReq.getProjectNodes());
        if (projectModifyReq.getDelayType().compareTo(1) >= 0) {
            ProjectNodeFlowDO projectNodeFlowDO = ProjectNodeFlowCopier.INSTANCE.convert(projectModifyReq.getProjectNodeFlow());
            projectNodeFlowDO.setProjectName(projectModifyReq.getName());
            projectNodeFlowComponent.process(projectNodeFlowDO, projectNodeDOList);
        } else if (Integer.valueOf(0).equals(projectModifyReq.getDelayType())) {
            //版本+1
            projectNodeFlowComponent.insertProjectNodeRecord(projectModifyReq.getId(), projectNodeDOList);
        } else {
            boolean match = projectNodeDOList.stream().anyMatch(e ->
                    ProjectNodeEnum.DEVELOP_START.getText().equals(e.getName()) && e.getActualDate() != null);
            if (match) {
                List<ProjectNodeRecordDO> list = projectNodeRecordMapper.list(projectModifyReq.getId());
                if (CollectionUtils.isEmpty(list)) {
                    //首次生成版本
                    projectNodeFlowComponent.insertProjectNodeRecord(projectModifyReq.getId(), projectNodeDOList);
                }
            }
        }
    }

    private void sendDingMsgIfPublishDateForward(Long id, Date oldPjEstablishPublishDate) {
        ProjectDO oldProjectDO = projectMapper.get(id);
        //此处已更新项目信息
        Date newPjEstablishPublishDate = oldProjectDO.getPjEstablishPublishDate();
        Date planEndDate = oldProjectDO.getPlanEndDate();
        log.info("立项预计上线时间提前:{}", oldProjectDO);
        List<ProjectNodeRecordDO> list = projectNodeRecordMapper.list(id);
        if (CollectionUtils.isEmpty(list) && newPjEstablishPublishDate != null
                && planEndDate != null && newPjEstablishPublishDate.before(planEndDate)
                && !ProjectStatusEnum.suspendOrTerminated(oldProjectDO.getStatus())
                && !Objects.equals(oldPjEstablishPublishDate, newPjEstablishPublishDate)) {
            messageEventPublisher.publish(new ProjectEstablishDateChangeMsgEvent(
                    this,
                    oldProjectDO.getId(),
                    oldProjectDO.getPmId(),
                    oldProjectDO.getName(),
                    DateUtil.parseToString(newPjEstablishPublishDate, DateStyle.YYYY_MM_DD))
            );
        }
    }

    /**
     * 项目名称校验
     *
     * @param projectName 项目名称
     */
    private void projectNameValidate(String projectName) {
        AssertUtil.checkState(!projectName.contains(CommonConstant.BLANK), "项目名称中请勿包含空格");

        ProjectDO byName = projectMapper.getByName(projectName);
        AssertUtil.checkState(byName == null, "该项目名称已存在,请修改后重试");
    }

    /**
     * 用户是否是查询人或者查询人的上级
     *
     * @param queryUserId 查询用户id
     * @return boolean
     */
    private boolean isLeader(String queryUserId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String localUserId = userInfo.getId();
        if (Objects.equals(queryUserId, localUserId)) {
            return true;
        }
        Set<String> allSuperiorByAccount = innerUserPersonClient.getAllSuperiorByAccount(queryUserId, false);
        return allSuperiorByAccount.contains(localUserId);
    }

}
