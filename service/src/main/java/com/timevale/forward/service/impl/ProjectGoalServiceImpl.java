package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProjectGoalMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectGoalDO;
import com.timevale.forward.facade.api.client.ProjectGoalService;
import com.timevale.forward.facade.api.request.ProjectGoalAddReq;
import com.timevale.forward.facade.api.request.ProjectGoalFinishReq;
import com.timevale.forward.facade.api.request.ProjectGoalModifyReq;
import com.timevale.forward.facade.api.result.ProjectGoalVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.ProjectGoalMD;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectGoalCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * create on 2022/6/21
 */
@Slf4j
@RestService
public class ProjectGoalServiceImpl implements ProjectGoalService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectGoalMapper projectGoalMapper;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public BaseResult<List<ProjectGoalVO>> list(Long projectGoalProjectId) {
        List<ProjectGoalVO> res = ProjectGoalCopier.INSTANCE.convert2VO(projectGoalMapper.getByProjectId(projectGoalProjectId));
        if (hasGoalFinishPermission()) {
            for (ProjectGoalVO goal : res) {
                goal.setPermitFinish(true);
            }
        }
        return BaseResult.success(res);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectGoalAddReq projectGoalAddReq) {
        // 基础数据存在校验
        Long projectId = projectGoalAddReq.getProjectId();
        String name = projectGoalAddReq.getName();
        AssertUtil.notNull(projectId, "项目id不能为空");
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "对应添加的项目不存在，请刷新后重试");
        // 权限校验
        AssertUtil.checkState(hasProjectEditPermission(getPermittedUserIds(project)), "您没有该操作权限");
        // 名称校验
        AssertUtil.checkState(Objects.isNull(projectGoalMapper.getByName(projectId, name)),
                "项目目标名称重复，请重新修改");
        if (ProjectGoalTypeEnum.QUANTIFY.getCode().equals(projectGoalAddReq.getType())) {
            AssertUtil.notNull(projectGoalAddReq.getReachValue(), "定量项目目标的目标达标值必填");
        } else {
            projectGoalAddReq.setReachValue(null);
        }
        ProjectGoalDO projectGoal = ProjectGoalCopier.INSTANCE.convert(projectGoalAddReq);
        // 新增数据默认不是主目标
        List<ProjectGoalDO> existsGoals = projectGoalMapper.getByProjectId(projectId);
        if (existsGoals.isEmpty()) {
            projectGoal.setIsMain(YesOrNoEnum.YES.getCode());
        } else {
            projectGoal.setIsMain(YesOrNoEnum.NO.getCode());
        }
        projectGoalMapper.insert(projectGoal);
        // 插入关联记录
        bizChangeLogMapper.insert(createCommonChangeLog()
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setField(BizChangeLogFieldEnum.PROJECT_GOAL.getText())
                .setMainId(projectId)
                .setIdentity(formIdentity(name))
                .setOldValue(name)
                .setNewValue(name)
                .setAction(ButtonActionEnum.LINK.getText())
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProjectGoalModifyReq projectGoalModifyReq) {
        ProjectGoalDO oldGoal = projectGoalMapper.get(projectGoalModifyReq.getId());
        AssertUtil.notNull(oldGoal, "您更改的项目目标不存在，请刷新后重试");
        ProjectGoalDO newGoal = ProjectGoalCopier.INSTANCE.convert(projectGoalModifyReq);
        // 权限校验
        ProjectDO project = projectMapper.get(oldGoal.getProjectId());
        AssertUtil.checkState(ProjectGoalStatusEnum.IN_PROGRESS.getCode().equals(oldGoal.getStatus()),
                "目标只有在进行中时可以修改");
        AssertUtil.checkState(hasProjectEditPermission(getPermittedUserIds(project)), "您没有该操作权限");
        String identity = oldGoal.getName();
        // 名称校验
        if (StringUtils.isNotEmpty(newGoal.getName()) && !oldGoal.getName().equals(newGoal.getName())) {
            AssertUtil.checkState(Objects.isNull(projectGoalMapper.getByName(project.getId(), newGoal.getName())),
                    "项目目标名称重复，请重新修改");
            identity = newGoal.getName();
            // 修改identity
            bizChangeLogMapper.updateIdentity(oldGoal.getProjectId(), formIdentity(oldGoal.getName()),
                    formIdentity(identity));
            // 修改项目目标名称
            bizChangeLogMapper.updateValue(oldGoal.getProjectId(), BizChangeLogFieldEnum.PROJECT_GOAL.getText(),
                    oldGoal.getName(), newGoal.getName());
        }
        // 定量、定性校验
        if (ProjectGoalTypeEnum.QUANTIFY.getCode().equals(newGoal.getType())) {
            AssertUtil.notNull(newGoal.getReachValue(), "定量项目目标的目标达标值必填");
        } else {
            newGoal.setReachValue(null);
        }
        // 更新目标
        projectGoalMapper.update(newGoal);
        // 生成修改记录
        ProjectGoalMD oldMd = ProjectGoalCopier.INSTANCE.convert(oldGoal);
        ProjectGoalMD newMd = ProjectGoalCopier.INSTANCE.convert(newGoal);
        List<BizChangeLogDO> logs = FieldCompareUtil.commonCompare(oldMd, newMd, BizChangeLogDO.class);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        for (BizChangeLogDO log : logs) {
            log.setMainId(project.getId());
            log.setIdentity(formIdentity(identity));
            log.setCreateManId(userInfo.getId());
            log.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        }
        if (ProjectGoalTypeEnum.QUALIFY.getCode().equals(newGoal.getType()) &&
                ProjectGoalTypeEnum.QUANTIFY.getCode().equals(oldGoal.getType())) {
            logs.add(createCommonChangeLog()
                    .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                    .setField(BizChangeLogFieldEnum.GOAL_REACH_VALUE.getText())
                    .setMainId(newGoal.getProjectId())
                    .setIdentity(formIdentity(newGoal.getName()))
                    .setOldValue(oldGoal.getReachValue().toPlainString())
                    .setNewValue(CommonConstant.NULL));
        }
        bizChangeLogMapper.batchInsert(logs);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(Long projectGoalId) {
        ProjectGoalDO goal = projectGoalMapper.get(projectGoalId);
        AssertUtil.notNull(goal, "您更改的项目目标不存在，请刷新后重试");
        ProjectDO project = projectMapper.get(goal.getProjectId());
        AssertUtil.checkState(hasProjectEditPermission(getPermittedUserIds(project)), "您没有该操作权限");
        AssertUtil.checkState(ProjectGoalStatusEnum.IN_PROGRESS.getCode().equals(goal.getStatus()),
                "目标只有在进行中时可以删除");
        List<ProjectGoalDO> goals = projectGoalMapper.getByProjectId(project.getId());
        goals.removeIf(g -> g.getId().equals(projectGoalId));
        // 只有一条数据时不允许删除
        AssertUtil.notEmpty(goals, "有目标的项目下至少需要保留一个项目目标");
        // 删除
        projectGoalMapper.delete(goal);
        if (YesOrNoEnum.YES.getCode().equals(goal.getIsMain())) {
            // 如果是删除主目标，重新设置一个主目标
            ProjectGoalDO newMainGoal = goals.get(0);
            newMainGoal.setIsMain(YesOrNoEnum.YES.getCode());
            // 插入主目标变更记录
            bizChangeLogMapper.insert(createCommonChangeLog()
                    .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                    .setField(BizChangeLogFieldEnum.MAIN_GOAL.getText())
                    .setMainId(goal.getProjectId())
                    .setIdentity(formIdentity(goal.getName()))
                    .setOldValue(YesOrNoEnum.NO.getText())
                    .setNewValue(YesOrNoEnum.YES.getText())
            );
            projectGoalMapper.update(newMainGoal);
        }
        // 插入取消关联记录
        bizChangeLogMapper.insert(createCommonChangeLog()
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setField(BizChangeLogFieldEnum.PROJECT_GOAL.getText())
                .setMainId(goal.getProjectId())
                .setIdentity(formIdentity(goal.getName()))
                .setOldValue(goal.getName())
                .setNewValue(goal.getName())
                .setAction(ButtonActionEnum.UN_LINK.getText())
        );
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> setMainGoal(Long projectGoalId) {
        ProjectGoalDO goal = projectGoalMapper.get(projectGoalId);
        AssertUtil.notNull(goal, "您更改的项目目标不存在，请刷新后重试");
        ProjectDO project = projectMapper.get(goal.getProjectId());
        AssertUtil.checkState(hasProjectEditPermission(getPermittedUserIds(project)), "您没有该操作权限");
        if (YesOrNoEnum.YES.getCode().equals(goal.getIsMain())) {
            return BaseResult.success(true);
        }
        List<ProjectGoalDO> oldGoals = projectGoalMapper.getByProjectId(project.getId());
        oldGoals.stream().filter(g -> YesOrNoEnum.YES.getCode().equals(g.getIsMain()))
                .findFirst().ifPresent(g -> {
                    g.setIsMain(YesOrNoEnum.NO.getCode());
                    projectGoalMapper.update(g);
                    // 插入主目标变更记录
                    bizChangeLogMapper.insert(createCommonChangeLog()
                            .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                            .setField(BizChangeLogFieldEnum.MAIN_GOAL.getText())
                            .setMainId(g.getProjectId())
                            .setIdentity(formIdentity(g.getName()))
                            .setOldValue(YesOrNoEnum.YES.getText())
                            .setNewValue(YesOrNoEnum.NO.getText())
                    );
                });
        ProjectGoalDO updateCond = new ProjectGoalDO();
        updateCond.setId(goal.getId());
        updateCond.setIsMain(YesOrNoEnum.YES.getCode());
        projectGoalMapper.update(updateCond);
        // 插入主目标变更记录
        bizChangeLogMapper.insert(createCommonChangeLog()
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setField(BizChangeLogFieldEnum.MAIN_GOAL.getText())
                .setMainId(goal.getProjectId())
                .setIdentity(formIdentity(goal.getName()))
                .setOldValue(YesOrNoEnum.NO.getText())
                .setNewValue(YesOrNoEnum.YES.getText())
        );
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> finish(ProjectGoalFinishReq projectGoalFinishReq) {
        ProjectGoalDO goal = projectGoalMapper.get(projectGoalFinishReq.getId());
        AssertUtil.notNull(goal, "您更改的项目目标不存在，请刷新后重试");
        AssertUtil.checkState(projectGoalFinishReq.getStatus().equals(ProjectGoalStatusEnum.FINISHED.getCode()) ||
                        projectGoalFinishReq.getStatus().equals(ProjectGoalStatusEnum.UNFINISHED.getCode()),
                "完成状态只能是 10-已完成 或 30-未完成");
        AssertUtil.checkState(hasGoalFinishPermission(), "您没有该操作权限");
        // 更新目标
        ProjectGoalDO updateCond = new ProjectGoalDO();
        updateCond.setId(projectGoalFinishReq.getId());
        updateCond.setStatus(projectGoalFinishReq.getStatus());
        updateCond.setCompleteNote(projectGoalFinishReq.getCompleteNote());
        projectGoalMapper.update(updateCond);
        // 添加完成状态变更记录
        bizChangeLogMapper.insert(createCommonChangeLog()
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setField(BizChangeLogFieldEnum.GOAL_STATUS.getText())
                .setMainId(goal.getProjectId())
                .setIdentity(formIdentity(goal.getName()))
                .setOldValue(ProjectGoalStatusEnum.IN_PROGRESS.getText())
                .setNewValue(ProjectGoalStatusEnum.FINISHED.getTextByCode(projectGoalFinishReq.getStatus()))
        );
        if (StringUtils.isNotBlank(projectGoalFinishReq.getCompleteNote())) {
            // 添加完成情况变更记录
            bizChangeLogMapper.insert(createCommonChangeLog()
                    .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                    .setField(BizChangeLogFieldEnum.GOAL_COMPLETE_NOTE.getText())
                    .setMainId(goal.getProjectId())
                    .setIdentity(formIdentity(goal.getName()))
                    .setOldValue(CommonConstant.NULL)
                    .setNewValue(projectGoalFinishReq.getCompleteNote())
            );
        }
        return BaseResult.success(true);
    }

    /**
     * 项目经理和产品经理以及他们的上级有权限编辑
     */
    private Set<String> getPermittedUserIds(ProjectDO project) {
        Set<String> permittedUserIds = personMapper.select(PersonListCondition.builder().mainId(project.getId())
                        .type(PersonTypeEnum.PROJECT_PD.getCode()).build())
                .stream().map(PersonDO::getUserId).collect(Collectors.toSet());
        permittedUserIds.add(project.getPmId());
        return permittedUserIds;
    }

    private boolean hasProjectEditPermission(Collection<String> permittedUserIds) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String userId = userInfo.getId();
        if (permittedUserIds.contains(userId)) {
            return true;
        }
        // 当前列表中无权限，则查询是否为上级修改
        for (String permittedUser : permittedUserIds) {
            Set<String> superiors = innerUserPersonClient.getAllSuperiorByAccount(permittedUser, false);
            if (superiors.contains(userId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * PMO 和 PMO 的上级才有权限编辑完成情况
     */
    private boolean hasGoalFinishPermission() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<BaseInfoResponse> users =
                innerUserPersonClient.getAllMyStaffWithSelfInfo(userInfo.getId(), false);
        for (BaseInfoResponse user : users) {
            if (CommonConstant.PMO.equalsIgnoreCase(user.getJob())) {
                return true;
            }
        }
        return false;
    }

    private BizChangeLogDO createCommonChangeLog() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        BizChangeLogDO bizChangeLogDO = new BizChangeLogDO();
        bizChangeLogDO.setCreateManId(userInfo.getId());
        bizChangeLogDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        return bizChangeLogDO;
    }

    private String formIdentity(String name) {
        return BizChangeLogFieldEnum.PROJECT_GOAL.getText() + CommonConstant.WIDE_COLON + name;
    }

}
