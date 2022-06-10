package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.client.ImprovementMeasureService;
import com.timevale.forward.facade.api.request.ImprovementMeasureModifyReq;
import com.timevale.forward.service.component.ImprovementMeasureComponent;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author by YangXu
 * @date 2022/06/10 11:20
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ImprovementMeasureServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ImprovementMeasureService improvementMeasureService;

    @Mock
    private ImprovementMeasureMapper improvementMeasureMapper;

    @Mock
    private DingWorkRecordClient dingWorkRecordClient;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private ImprovementMeasureComponent improvementMeasureComponent;

    @Test
    public void testAdd(){
        doNothing().when(improvementMeasureComponent).add(any());
        assert improvementMeasureService.add(any()).ifSuccess();
    }

    @Test
    public void testModify(){
        ImprovementMeasureDO improvementMeasureDO = new ImprovementMeasureDO();
        improvementMeasureDO.setTodo(true);
        improvementMeasureDO.setTodoId("12345");
        improvementMeasureDO.setExecutor("executor");
        improvementMeasureDO.setExecutor("executorId");
        when(improvementMeasureMapper.selectByCondition(any())).thenReturn(Collections.singletonList(improvementMeasureDO));

        doNothing().when(improvementMeasureComponent).updateTodoTask(any());
        doNothing().when(improvementMeasureComponent).deleteTodoTask(any());
        doNothing().when(improvementMeasureComponent).addTodoTask(any());

        when(improvementMeasureMapper.update(any())).thenReturn(1);

        ImprovementMeasureModifyReq req = new ImprovementMeasureModifyReq();
        req.setImplementationTime(new Date());

        req.setTodo(false);
        assert improvementMeasureService.modify(req).ifSuccess();

        req.setTodo(true);
        req.setExecutor("executor");
        req.setExecutor("executorId");
        assert improvementMeasureService.modify(req).ifSuccess();

        req.setTodo(true);
        req.setExecutor("executor-diff");
        req.setExecutor("executorId-diff");
        assert improvementMeasureService.modify(req).ifSuccess();
    }

}
