package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.ProductDemandLinkTaskQueryList;
import com.timevale.forward.facade.api.query.TaskLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskProductDemandQueryList;
import com.timevale.forward.facade.api.query.TaskQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.TaskDetailVO;
import com.timevale.forward.facade.api.result.TaskListVO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.mandarin.base.util.FieldUtils;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * @author xingyun
 * @date 2022-03-16 16:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class TaskServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private TaskServiceImpl taskServiceImp;

    @Mock
    private TaskComponent taskComponent;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskTimeMapper taskTimeMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    private TaskProductDemandComponent taskProductDemandComponent;

    @Mock
    private FileComponent fileComponent;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private TaskProductDemandMapper taskProductDemandMapper;

    @Mock
    private ProductDemandComponent productDemandComponent;

    @Mock
    private ElapsedTimeClient elapsedTimeClient;

    @Mock
    private TaskTimeComponent taskTimeComponent;

    @Mock
    MessageEventPublisher messageEventPublisher;

    @Mock
    private ProjectNodeMapper projectNodeMapper;


    @Test
    public void testList() {
        TaskQueryList taskQueryList = new TaskQueryList();
        taskQueryList.setAscription("CURRENT_USER");
        when(personMapper.getMainIds(any(),any(),any())).thenReturn(Collections.emptyList());
        BaseResult<PageQueryResult<TaskVO>> baseResult = taskServiceImp.list(taskQueryList);

        assert baseResult.ifSuccess();
        taskQueryList.setAscription("TEAM");
        when(innerUserPersonClient.getAllMyStaffWithSelf(any(),any())).thenReturn(Collections.singletonList("1"));
        baseResult = taskServiceImp.list(taskQueryList);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testAdd() {
        TaskAddReq taskAddReq = new TaskAddReq();
        taskAddReq.setExecutors(Collections.singletonList(new PersonAddReq()));
        taskAddReq.setProductDemandIds(Collections.emptyList());
        taskAddReq.setPlanUseTime(new BigDecimal("1"));
        taskAddReq.setTodo(true);
        taskAddReq.setActualStartDate(new Date());
        taskAddReq.setActualEndDate(new Date());
        taskAddReq.setName("");
        when(taskMapper.get(any())).thenReturn(null);
        when(projectNodeMapper.get(any())).thenReturn(Collections.emptyList());
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Collections.emptyList());
        FieldUtils.setFieldValue("excludeBizDomain", taskServiceImp, "1");
        when(productLineMapper.getPlineAndBizDomain(any())).thenReturn(Lists.newArrayList(new ProjectProductLineBizDomain()));
        doNothing().when(taskComponent).addTodoTask(any(),any(),any());
        when(taskMapper.insert(any())).thenReturn(0);
        TaskTimeDO t1 = new TaskTimeDO();
        t1.setId(1L);
        t1.setStartDate(new Date());
        t1.setEndDate(new Date());
        TaskTimeDO t2 =new TaskTimeDO();
        t1.setId(2L);
        t2.setStartDate(new Date());
        t2.setEndDate(new Date());
        when(taskTimeMapper.list(any())).thenReturn(Lists.newArrayList(t1, t2));
        when(elapsedTimeClient.getElapsedTime(any(),any())).thenReturn(1L);
        doNothing().when(taskTimeComponent).updateById(any(),any());
        when(taskTimeMapper.delete(any(),any())).thenReturn(0);
        doNothing().when(fileComponent).add(any(),any(),any());
        doNothing().when(personComponent).add(any(),any(),any());
        doNothing().when(taskProductDemandComponent).batchInsert(any(),any());
        ProjectDO projectDO=new ProjectDO();
        projectDO.setPmId("");
        when(projectMapper.get(any())).thenReturn(projectDO);
        doNothing().when(messageEventPublisher).publish(any());
        BaseResult<Long> baseResult = taskServiceImp.add(taskAddReq);
        assert baseResult.ifSuccess();
    }
    @Test
    public void testModify() {
        TaskModifyReq taskModifyReq = new TaskModifyReq();
        taskModifyReq.setId(1L);
        taskModifyReq.setExecutors(Collections.singletonList(new PersonAddReq()));
        taskModifyReq.setProductDemandIds(Collections.emptyList());
        taskModifyReq.setPlanUseTime(new BigDecimal("1"));
        taskModifyReq.setTodo(true);
        taskModifyReq.setActualStartDate(new Date());
        taskModifyReq.setActualEndDate(new Date());
        TaskDO taskDO=new TaskDO();
        taskDO.setId(1L);
        taskDO.setStatus(TaskStatusEnum.WAITING.getCode());
        when(taskMapper.get(any())).thenReturn(taskDO);
        when(projectNodeMapper.get(any())).thenReturn(Collections.emptyList());
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Collections.emptyList());
        FieldUtils.setFieldValue("excludeBizDomain", taskServiceImp, "1");
        when(productLineMapper.getPlineAndBizDomain(any())).thenReturn(Lists.newArrayList(new ProjectProductLineBizDomain()));
        doNothing().when(taskComponent).addTodoTask(any(),any(),any());
        when(taskMapper.insert(any())).thenReturn(0);
        TaskTimeDO t1 = new TaskTimeDO();
        t1.setId(1L);
        t1.setStartDate(new Date());
        t1.setEndDate(new Date());
        TaskTimeDO t2 =new TaskTimeDO();
        t1.setId(2L);
        t2.setStartDate(new Date());
        t2.setEndDate(new Date());
        when(taskTimeMapper.list(any())).thenReturn(Lists.newArrayList(t1, t2));
        when(elapsedTimeClient.getElapsedTime(any(),any())).thenReturn(1L);
        doNothing().when(taskTimeComponent).updateById(any(),any());
        when(taskTimeMapper.delete(any(),any())).thenReturn(0);
        doNothing().when(fileComponent).add(any(),any(),any());
        doNothing().when(personComponent).add(any(),any(),any());
        doNothing().when(taskProductDemandComponent).batchInsert(any(),any());
        ProjectDO projectDO=new ProjectDO();
        projectDO.setPmId("");
        when(projectMapper.get(any())).thenReturn(projectDO);
        doNothing().when(messageEventPublisher).publish(any());
        BaseResult<Boolean> baseResult = taskServiceImp.modify(taskModifyReq);
        assert baseResult.ifSuccess();

    }

    @Test
    public void testGet() {
        TaskDO taskDO=new TaskDO();
        taskDO.setId(1L);
        taskDO.setStatus(TaskStatusEnum.DONE.getCode());
        when(taskMapper.get(any())).thenReturn(taskDO);
        when(projectMapper.get(any())).thenReturn(new ProjectDO());
        when(productLineMapper.selectById(any())).thenReturn(new ProductLineDO());
        when(fileComponent.select(anyLong(),any())).thenReturn(Collections.emptyList());
        when(personComponent.select(any(),any())).thenReturn(Collections.emptyList());
        when(taskTimeComponent.getUseTime(any())).thenReturn(Collections.emptyList());
        BaseResult<TaskDetailVO> baseResult = taskServiceImp.get(1L);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testUpdateStatus() {
        TaskDO taskDO=new TaskDO();
        taskDO.setId(1L);
        taskDO.setStatus(TaskStatusEnum.WAITING.getCode());
        when(taskMapper.get(any())).thenReturn(taskDO);
        doNothing().when(taskTimeComponent).updateEndDate(any(),any());
        doNothing().when(taskComponent).deleteTodoTask(any());
        when(taskMapper.update(any())).thenReturn(1);
        BaseResult<Boolean> baseResult = taskServiceImp.updateStatus(1L,-10);

        assert baseResult.ifSuccess();
        doNothing().when(taskProductDemandComponent).update(any(),any());
        when(taskTimeMapper.delete(any(),any())).thenReturn(1);
        baseResult = taskServiceImp.updateStatus(1L,-20);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testEnable() {
        TaskDO taskDO=new TaskDO();
        taskDO.setId(1L);
        taskDO.setStatus(TaskStatusEnum.SUSPEND.getCode());
        taskDO.setTodo(true);
        when(taskMapper.get(any())).thenReturn(taskDO);
        when(personComponent.select(any(),any())).thenReturn(Collections.emptyList());
        doNothing().when(taskComponent).addTodoTask(any(),any(),any());
        BaseResult<Boolean> baseResult = taskServiceImp.enable(1L);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testExecute() {
        TaskDO taskDO=new TaskDO();
        taskDO.setId(1L);
        taskDO.setStatus(TaskStatusEnum.WAITING.getCode());
        when(taskMapper.getById(any())).thenReturn(taskDO);
        BaseResult<Boolean> baseResult = taskServiceImp.execute(new TaskExecuteReq());
        assert baseResult.ifSuccess();
    }

    @Test
    public void testDone() {
        TaskDO taskDO=new TaskDO();
        taskDO.setId(1L);
        taskDO.setTodo(true);
        taskDO.setStatus(TaskStatusEnum.PROGRESS.getCode());
        when(taskMapper.getById(any())).thenReturn(taskDO);
        TaskTimeDO taskTimeDO=new TaskTimeDO();
        taskTimeDO.setStartDate(new Date(1));
        taskTimeDO.setEndDate(new Date(2));
        when(taskTimeMapper.list(any())).thenReturn(Collections.singletonList(taskTimeDO));
        ProjectDO projectDO=new ProjectDO();
        projectDO.setPmId("");
        when(projectMapper.get(any())).thenReturn(projectDO);
        doNothing().when(messageEventPublisher).publish(any());

        BaseResult<Boolean> baseResult = taskServiceImp.done(new TaskDoneReq());
        assert baseResult.ifSuccess();
    }

    @Test
    public void testMatchProductDemandList() {
        TaskLinkProductDemandQueryList queryList=new TaskLinkProductDemandQueryList();
        queryList.setId(1L);
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Collections.singletonList(new ProjectProductDemandDO(){{ setProductDemandId(1L); }}));
        when(taskProductDemandMapper.get(any())).thenReturn(Collections.singletonList(new TaskProductDemandDO(){{setProductDemandId(1L);}}));
        when(productDemandComponent.list(any())).thenReturn(Collections.singletonList(new ProductDemandListDO()));
        BaseResult<PageQueryResult<ProductDemandVO>> baseResult = taskServiceImp.matchProductDemandList(queryList);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testLinkOrUnLinkProductDemand() {
        TaskProductDemandLinkReq taskProductDemandLinkReq=new TaskProductDemandLinkReq();
        taskProductDemandLinkReq.setProductDemandIds(Collections.singletonList(1L));
        doNothing().when(taskProductDemandComponent).update(any(),any());
        BaseResult<Boolean>  baseResult = taskServiceImp.linkOrUnLinkProductDemand(taskProductDemandLinkReq);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testLinkProductDemandList() {
        TaskProductDemandQueryList taskProductDemandLinkReq=new TaskProductDemandQueryList();
        when(taskProductDemandMapper.linkProductDemandList(any())).thenReturn(Collections.singletonList(new ProductDemandListDO()));
        BaseResult<PageQueryResult<ProductDemandVO>> baseResult = taskServiceImp.linkProductDemandList(taskProductDemandLinkReq);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testGetElapsedTime() {
        ElapsedTimeQueryReq elapsedTimeQueryReq=new ElapsedTimeQueryReq();
        when(taskComponent.getElapsedTime(any(),any())).thenReturn(BigDecimal.ZERO);
        BaseResult<BigDecimal> baseResult = taskServiceImp.getElapsedTime(elapsedTimeQueryReq);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testListTask() {
        ProductDemandLinkTaskQueryList taskQueryList = new ProductDemandLinkTaskQueryList();
        List<TaskDO> taskDOList=Lists.newArrayList(new TaskDO(){{setId(1L);}});
        when(taskMapper.getByProductDemandId(any(),any())).thenReturn(taskDOList);
        List<PersonDO> executorList=Lists.newArrayList(new PersonDO(){{setMainId(1L);}});
        when(personMapper.get(any(),any())).thenReturn(executorList);
        BaseResult<PageQueryResult<TaskListVO>> baseResult = taskServiceImp.listTask(taskQueryList);
        assert baseResult.ifSuccess();
    }

}





























