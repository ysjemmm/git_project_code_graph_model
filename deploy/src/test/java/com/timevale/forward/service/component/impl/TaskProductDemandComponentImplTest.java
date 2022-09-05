package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.TaskProductDemandMapper;
import com.timevale.forward.dal.entity.TaskProductDemandDO;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class TaskProductDemandComponentImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private TaskProductDemandComponentImpl taskProductDemandComponent;

    @Mock
    private TaskProductDemandMapper taskProductDemandMapper;


    @Test
    public void testBatchInsert() {
        when(taskProductDemandMapper.get(any())).thenReturn(Lists.newArrayList(new TaskProductDemandDO(){{setProductDemandId(1L);}}));
        taskProductDemandComponent.batchInsert(1L,Lists.newArrayList(2L));
    }

    @Test
    public void testUpdate() {
        taskProductDemandComponent.update(null,null);
    }
}

































