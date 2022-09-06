package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.TaskTimeMapper;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.superset.config.DistributeConfig;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class TaskTimeComponentImplTest extends AbstractTestNGSpringContextTests {

    @Spy
    @InjectMocks
    private TaskTimeComponentImpl taskTimeComponent;

    @Mock
    private TaskTimeMapper taskTimeMapper;

    @Mock
    private DistributeConfig distributeConfig;

    @Mock
    private PersonComponent personComponent;


    @Test
    public void testUpdateEndDate() {
        taskTimeComponent.updateEndDate(null,null);
    }

    @Test
    public void testInsert() {
        taskTimeComponent.insert(null,null,null);
    }

    @Test
    public void testGetUseTime() {
        Mockito.when(distributeConfig.getTaskUseTime()).thenReturn(null);
        Mockito.doReturn(new ArrayList<>()).when(taskTimeComponent).doGet(Mockito.any());
        taskTimeComponent.getUseTime(new TaskDO(){{setId(1L);}});
    }

    @Test
    public void testUpdateById() {
        taskTimeComponent.updateById(null,null);
    }

}

































