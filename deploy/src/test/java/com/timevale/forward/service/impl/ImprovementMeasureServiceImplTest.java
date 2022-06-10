package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.request.ImprovementMeasureCompleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureDeleteReq;
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
import java.util.HashMap;
import java.util.Map;

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
    private ImprovementMeasureServiceImpl improvementMeasureService;

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
        improvementMeasureDO.setExecutorId("executorId");
        when(improvementMeasureMapper.selectByCondition(any())).thenReturn(Collections.singletonList(improvementMeasureDO));

        doNothing().when(improvementMeasureComponent).updateTodoTask(any());
        doNothing().when(improvementMeasureComponent).deleteTodoTask(any());

        when(improvementMeasureComponent.addTodoTask(any())).thenReturn("1");
        when(improvementMeasureMapper.update(any())).thenReturn(1);

        ImprovementMeasureModifyReq req = new ImprovementMeasureModifyReq();
        req.setImplementationTime(new Date());

        req.setTodo(true);
        req.setExecutor("executor");
        req.setExecutorId("executorId");
        assert improvementMeasureService.modify(req).ifSuccess();

        req.setTodo(true);
        req.setExecutor("executor-diff");
        req.setExecutorId("executorId-diff");
        assert improvementMeasureService.modify(req).ifSuccess();

        improvementMeasureDO.setTodo(false);
        when(improvementMeasureMapper.selectByCondition(any())).thenReturn(Collections.singletonList(improvementMeasureDO));
        assert  improvementMeasureService.modify(req).ifSuccess();
    }

    @Test
    public void testDelete(){
        ImprovementMeasureDeleteReq req = new ImprovementMeasureDeleteReq();
        req.setId(1L);
        doNothing().when(improvementMeasureComponent).delete(any());
        assert improvementMeasureService.delete(req).ifSuccess();
    }

    @Test
    public void testComplete(){
        ImprovementMeasureCompleteReq req = new ImprovementMeasureCompleteReq();
        req.setId(1L);

        ImprovementMeasureDO improvementMeasureDO = new ImprovementMeasureDO();
        improvementMeasureDO.setName("1");
        improvementMeasureDO.setTodo(true);
        improvementMeasureDO.setExecutorId("1");
        improvementMeasureDO.setImplementationTime(new Date());
        when(improvementMeasureMapper.selectByCondition(any())).thenReturn(Collections.singletonList(improvementMeasureDO));
        when(improvementMeasureMapper.update(any())).thenReturn(1);

        Map<String, String> unionIdMap = new HashMap<>();
        unionIdMap.put("1","1");
        when(innerUserPersonClient.getUnionIds(any())).thenReturn(unionIdMap);

        doNothing().when(dingWorkRecordClient).updateTask(any());

        assert improvementMeasureService.complete(req).ifSuccess();
    }

}
