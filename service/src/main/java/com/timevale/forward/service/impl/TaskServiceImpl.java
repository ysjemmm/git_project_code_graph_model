package com.timevale.forward.service.impl;

import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.request.TaskModifyReq;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class TaskServiceImpl implements TaskService {

    @Resource
    private TaskComponent taskComponent;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private PersonMapper personMapper;

    @Override
    public BaseResult<PageQueryResult<TaskVO>> list(TaskQueryList taskQueryList) {

        log.info("任务列表接收参数:{}", taskQueryList);
        String currentUser = LocalSessionUtils.getUserInfo().getId();
        TaskListCondition condition = TaskCopier.INSTANCE.convert(taskQueryList);
        List<Long> taskIds = new ArrayList<>();
        //1.查找我或我的团队所属任务id
        if (AscriptionEnum.CURRENT_USER.name().equals(taskQueryList.getAscription())) {
            taskIds = personMapper.getMainIds(Lists.newArrayList(currentUser), null, PersonTypeEnum.TASK_EXECUTOR.getCode());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

        } else if (AscriptionEnum.TEAM.name().equals(taskQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            taskIds = personMapper.getMainIds(allMyStaffWithSelf, null, PersonTypeEnum.TASK_EXECUTOR.getCode());
            if (CollectionUtils.isEmpty(taskIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }

        return taskComponent.page(condition, taskIds);

//        PageQueryResult<TaskVO> pageQueryResult = new PageQueryResult<>();
//        PageInfo<ProjectListDO> pageInfo = new PageInfo<>(projectDO);
//        pageQueryResult.setResultList(projectVO);
//        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
//        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> add(TaskAddReq taskAddReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(TaskModifyReq taskModifyReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TaskDetailVO> get(Long taskId) {
        TaskDetailVO taskDetailVO=new TaskDetailVO();
        return BaseResult.success(taskDetailVO);
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long taskId, Integer type) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> enable(Long taskId) {
        return BaseResult.success(true);
    }
}
