package com.timevale.forward.service.component.impl;

import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.lowcode.support.response.task.TaskHandleUserResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/21 16:31
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectFlowComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProjectFlowComponentImpl projectFlowComponent;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private EpeiusClient epeiusClient;

    @Mock
    private ProjectFlowMapper projectFlowMapper;

    @Mock
    private ProjectNodeMapper projectNodeMapper;

    @Mock
    private ProjectComponent projectComponent;


    @Test
    public void testUpdateFlowInfo() {
        ProcessResponse response=new ProcessResponse();
        response.setCurrentTaskIdList(Lists.newArrayList("1"));
        response.setProcessStatus(FlowStatusEnum.FLOW_COMPLETE.getValue());
        when(epeiusClient.getProcessInfo(any())).thenReturn(response);
        when(projectMapper.get(any())).thenReturn(new ProjectDO());
        when(projectFlowMapper.get(any(),any())).thenReturn(new ProjectFlowDO(){{
            setReview("[\"1\"]");
            setReviewId("[\"1\"]");
        }});
        when(projectNodeMapper.getByName(any(),any())).thenReturn(new ProjectNodeDO());
        when(projectComponent.getStatus(any())).thenReturn(1);
        TaskHandleUserResponse userResponse=new TaskHandleUserResponse();
        userResponse.setPassedUserList(Lists.newArrayList());
        userResponse.setRejectUserList(Lists.newArrayList());
        when(epeiusClient.getTaskHandleUserList(any())).thenReturn(userResponse);
        projectFlowComponent.updateFlowInfo("1");
    }
}

































