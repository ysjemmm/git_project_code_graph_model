package com.timevale.forward.service.component.impl;

import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.result.TaskVO;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.component.TaskTimeComponent;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


/**
 * @author xingyun
 * @date 2022-03-16 16:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class TaskComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private TaskComponentImpl taskComponentImpl;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskProductDemandComponent taskProductDemandComponent;

    @Mock
    private ElapsedTimeClient elapsedTimeClient;

    @Mock
    private TaskTimeComponent taskTimeComponent;

    @Mock
    private TaskTimeMapper taskTimeMapper;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private DingWorkRecordClient dingWorkRecordClient;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;


    @Test
    public void testPage() {
        TaskListCondition condition = TaskListCondition.builder().build();
        List<Long> taskIds=Collections.singletonList(1L);
        condition.setExecutorIds(Collections.singletonList(""));
        condition.setProjectIds(Collections.singletonList(1L));
        condition.setProductLineIds(Collections.singletonList(1L));
        when(personMapper.getMainIds(any(),any(),any())).thenReturn(taskIds);
        when(taskMapper.getByProjectIds(any(),any())).thenReturn(taskIds);
        when(taskMapper.getByProductLineIds(any(),any())).thenReturn(taskIds);
        when(taskMapper.list(any())).thenReturn(Collections.singletonList(new TaskDO(){{setProjectId(1L);setPlanEndDate(new Date());}}));
        when(personMapper.get(any(),any())).thenReturn(Collections.singletonList(new PersonDO(){{setMainId(1L);}}));
        when(productLineMapper.selectByIds(any())).thenReturn(Collections.singletonList(new ProductLineDO(){{setName("");}}));
        when(projectMapper.getByIds(any())).thenReturn(Collections.singletonList(new ProjectDO(){{setId(1L);}}));
        BaseResult<PageQueryResult<TaskVO>> baseResult = taskComponentImpl.page(condition,taskIds);
        assert baseResult.ifSuccess();
    }

    @Test
    public void testUpdateStatusAsProjectStatusChange() {
        TaskDO taskDO = new TaskDO();
        taskDO.setStatus(0);
        taskDO.setTodoId("1");
        List<TaskDO> taskDOList=Collections.singletonList(taskDO);
        when(taskMapper.getByProjectId(any())).thenReturn(taskDOList);
        when(taskMapper.updateStatusAsProjectStatusChange(any())).thenReturn(1);
        Map<String, String> map= Maps.newHashMap();
        map.put("1","1");
        when(innerUserPersonClient.getUnionIds(any())).thenReturn(map);
        taskComponentImpl.updateStatusAsProjectStatusChange(1L,-10,true);
    }

    @Test
    public void testGetElapsedTime() {
        when(elapsedTimeClient.getElapsedTime(any(),any())).thenReturn(1L);
        taskComponentImpl.getElapsedTime(null,null);
    }

    @Test
    public void testContainProductLineInTask() {
        when(taskMapper.getByProjectId(any())).thenReturn(Collections.singletonList(new TaskDO(){{setProductLineId(1L);}}));
        taskComponentImpl.containProductLineInTask(1L,Collections.singletonList(1L));
    }

    @Test
    public void testAddTodoTask() {
        TaskDO taskDO=new TaskDO();
        taskDO.setPlanEndDate(new Date());
        Map<String, String> map= Maps.newHashMap();
        map.put("1","1");
        when(innerUserPersonClient.getUnionIds(any())).thenReturn(map);
        taskComponentImpl.addTodoTask(taskDO, Lists.newArrayList("1"));
    }

    @Test
    public void testUpdateTodoTaskk() {
        TaskDO taskDO=new TaskDO();
        taskDO.setPlanEndDate(new Date());
        Map<String, String> map= Maps.newHashMap();
        map.put("1","1");
        when(innerUserPersonClient.getUnionIds(any())).thenReturn(map);
        taskComponentImpl.updateTodoTask(taskDO, Lists.newArrayList("1"));
    }

    @Test
    public void testDeleteTodoTask() {
        TaskDO taskDO=new TaskDO();
        taskDO.setPlanEndDate(new Date());
        Map<String, String> map= Maps.newHashMap();
        map.put("1","1");
        when(innerUserPersonClient.getUnionIds(any())).thenReturn(map);
        taskComponentImpl.deleteTodoTask("1");
    }


}





























