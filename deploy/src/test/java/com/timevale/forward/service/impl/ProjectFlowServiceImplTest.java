package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProjectFlowAddReq;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectFlowComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.mandarin.base.util.FieldUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectFlowServiceImplTest extends AbstractTestNGSpringContextTests {

    @Mock
    private EpeiusClient epeiusClient;

    @Mock
    private ProjectNodeMapper projectNodeMapper;

    @Mock
    private ProjectFlowMapper projectFlowMapper;

    @Mock
    private ProjectComponent projectComponent;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectLogComponent projectLogComponent;

    @Mock
    private ProjectFlowComponent projectFlowComponent;

    @InjectMocks
    private ProjectFlowServiceImpl projectFlowService;

    @Test
    public void testAdd() {

        ProjectFlowAddReq flowAddReq=new ProjectFlowAddReq();
        PersonAddReq personAddReq=new PersonAddReq();
        personAddReq.setUserId("1");
        flowAddReq.setProposer(personAddReq);
        flowAddReq.setReviews(Lists.newArrayList(personAddReq));

        List<ProjectFlowDO> list= Lists.newArrayList(new ProjectFlowDO());
        when(projectMapper.get(any())).thenReturn(new ProjectDO());
        when(projectFlowMapper.getByProjectId(any())).thenReturn(list);
        when(projectNodeMapper.getByName(any(),any())).thenReturn(new ProjectNodeDO());
        when(projectComponent.getStatus(any())).thenReturn(1);
        FieldUtils.setFieldValue("domainName", projectFlowService, "1");
        when(epeiusClient.start(any())).thenReturn("");
        assert projectFlowService.add(flowAddReq).ifSuccess();
    }

}

































