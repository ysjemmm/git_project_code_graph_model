package com.timevale.forward.service.impl;

import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskCondition;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.dal.entity.ProjectProductLineBizDomain;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.dal.entity.TaskTimeDO;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.request.TaskModifyReq;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.TaskComponent;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskTimeMapper taskTimeMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;

    @Resource
    private FileComponent fileComponent;

    public static final String PRIVATE_CLOUD="私有云";

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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(TaskAddReq taskAddReq) {
        log.info("任务新增接收参数:{}", taskAddReq);
        TaskDO taskDO = TaskCopier.INSTANCE.convert(taskAddReq);
        checkNameExisted(taskDO);
        checkProductDemandIdsByProjectLinked(taskDO);
        checkPlanDate(taskDO);
        taskDO.setTodoId(StringUtils.EMPTY);
        if(taskAddReq.getTodo()){
            //钉钉待办
        }
        taskMapper.insert(taskDO);

        insertTaskTime(taskDO);

        fileComponent.add(taskAddReq.getFiles(), taskDO.getId(), FileTypeEnum.TASK.getCode());

        taskProductDemandComponent.batchInsert(taskDO.getId(),taskAddReq.getProductDemandIds());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TaskModifyReq taskModifyReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TaskDetailVO> get(Long taskId) {
        TaskDetailVO taskDetailVO=new TaskDetailVO();
        return BaseResult.success(taskDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(Long taskId, Integer type) {
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> enable(Long taskId) {
        return BaseResult.success(true);
    }

    /**
     * 名称重复
     * @param taskDO
     */
    private void checkNameExisted(TaskDO taskDO){
        TaskCondition condition = TaskCondition.builder().projectId(taskDO.getProjectId()).name(taskDO.getName()).build();
        TaskDO existTaskDO = taskMapper.get(condition);
        if(existTaskDO!=null){
            throw new BaseBizRuntimeException("该任务名称已存在,请修改后重试");
        }
    }

    /**
     * 只能关联本项目下的产品需求
     * @param taskDO
     */
    private void checkProductDemandIdsByProjectLinked(TaskDO taskDO){
        List<Long> existProductDemandIds = projectProductDemandMapper.getByProjectId(taskDO.getProjectId())
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        List<Long> productDemandIds = taskDO.getProductDemandIds();
        productDemandIds.removeAll(existProductDemandIds);
        if(!CollectionUtils.isEmpty(productDemandIds)){
            throw new BaseBizRuntimeException("产品需求id:"+productDemandIds+"没有被该项目关联,请刷新后重试");
        }
    }

    private void checkPlanDate(TaskDO taskDO){
        List<String> bizDomainNames = productLineMapper.getByProjectIds(Lists.newArrayList(taskDO.getProjectId()))
                .stream().map(ProjectProductLineBizDomain::getBizDomainName).collect(Collectors.toList());
        if(!bizDomainNames.contains(PRIVATE_CLOUD)){
            //除私有云业务域外,计划时间不能超过16h
        }
    }
    private void insertTaskTime(TaskDO taskDO){
        if(taskDO.getActualStartDate()==null&&taskDO.getActualEndDate()==null){
            taskDO.setStatus(TaskStatusEnum.WAITING.getCode());
        }else if (taskDO.getActualStartDate()!=null&&taskDO.getActualEndDate()==null){
            taskDO.setStatus(TaskStatusEnum.PROGRESS.getCode());
        }else{
            //需计算实际耗时
            taskDO.setStatus(TaskStatusEnum.DONE.getCode());
        }
        if(!TaskStatusEnum.WAITING.getCode().equals(taskDO.getStatus())){
            TaskTimeDO taskTimeDO=new TaskTimeDO();
            taskTimeDO.setTaskId(taskDO.getId());
            taskTimeDO.setStartDate(taskDO.getActualStartDate());
            taskTimeDO.setEndDate(taskDO.getActualEndDate());
            taskTimeMapper.insert(taskTimeDO);
        }
    }


}
