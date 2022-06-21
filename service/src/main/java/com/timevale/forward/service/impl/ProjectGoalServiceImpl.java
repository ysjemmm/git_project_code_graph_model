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
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.copy.ProjectGoalCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
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
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectGoalAddReq projectGoalAddReq) {
        // 基础数据存在校验
        Long projectId = projectGoalAddReq.getProjectId();
        String name = projectGoalAddReq.getName();
        AssertUtil.notNull(projectId, "项目id不能为空");
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "对应添加的项目不存在，请刷新后重试");
        AssertUtil.checkState(YesOrNoEnum.YES.getCode().equals(project.getIsWithGoal()),
                "该项目无项目目标，无法新增项目目标");
        // 权限校验
        AssertUtil.checkState(hasProjectEditPermission(getPermittedUserIds(project)), "您没有该操作权限");
        // 名称校验
        AssertUtil.checkState(Objects.isNull(projectGoalMapper.getByName(name)),
                "项目目标名称重复，请重新修改");
        if (YesOrNoEnum.YES.getCode().equals(projectGoalAddReq.getIsMain())) {
            // 传入为主目标则撤销当前主目标
            projectGoalMapper.unsetMainGoal(projectId);
        }
        ProjectGoalDO projectGoal = ProjectGoalCopier.INSTANCE.convert(projectGoalAddReq);
        projectGoalMapper.insert(projectGoal);
        // 插入关联记录
        bizChangeLogMapper.insert(createCommonChangeLog()
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setField(BizChangeLogFieldEnum.PROJECT_GOAL.getText())
                .setMainId(projectId)
                .setOldValue(name)
                .setNewValue(name)
                .setAction(ButtonActionEnum.LINK.getText())
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(ProjectGoalModifyReq projectGoalModifyReq) {
        return null;
    }

    @Override
    public BaseResult<Boolean> delete(Long projectGoalId) {
        // 只有一条数据时不允许删除
        return null;
    }

    @Override
    public BaseResult<Boolean> setMainGoal(Long projectGoalId) {
        return null;
    }

    @Override
    public BaseResult<Boolean> finish(ProjectGoalFinishReq projectGoalFinishReq) {
        return null;
    }

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

    private BizChangeLogDO createCommonChangeLog() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        BizChangeLogDO bizChangeLogDO = new BizChangeLogDO();
        bizChangeLogDO.setCreateManId(userInfo.getId());
        bizChangeLogDO.setCreateMan(userInfo.getAlias());
        return bizChangeLogDO;
    }

}
