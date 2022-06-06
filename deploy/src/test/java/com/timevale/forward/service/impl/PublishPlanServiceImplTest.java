package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.ProjectPublishPlanMapper;
import com.timevale.forward.dal.dto.PublishPlanDTO;
import com.timevale.forward.dal.dto.PublishPlanResultDTO;
import com.timevale.forward.dal.entity.ProjectPublishPlanDO;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;
import com.timevale.forward.facade.api.request.ProjectPublishPlanLinkReq;
import com.timevale.forward.service.component.ProjectPublishPlanComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
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
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class PublishPlanServiceImplTest extends AbstractTestNGSpringContextTests {

    @Mock
    private EpeiusClient epeiusClient;

    @Mock
    private PublishPlatformClient publishPlatformClient;

    @Mock
    private ProjectPublishPlanMapper projectPublishPlanMapper;

    @Mock
    private ProjectPublishPlanComponent projectPublishPlanComponent;

    @InjectMocks
    private PublishPlanServiceImpl publishPlanService;

    @Test
    public void testLinkPublishPlanList() {
        PublishPlanQueryList publishPlanQueryList=new PublishPlanQueryList();
        PublishPlanResultDTO dto=new PublishPlanResultDTO();
        dto.setList(Lists.newArrayList(new PublishPlanDTO()));
        dto.setCount(1);
        when(projectPublishPlanMapper.get(any())).thenReturn(Lists.newArrayList(new ProjectPublishPlanDO(){{
            setPublishPlanId(1L);
        }}));
        when(publishPlatformClient.list(any())).thenReturn(dto);
        assert publishPlanService.linkPublishPlanList(publishPlanQueryList).ifSuccess();
    }

    @Test
    public void testLinkOrUnLinkPublishPlan() {
        ProjectPublishPlanLinkReq req=new ProjectPublishPlanLinkReq();
        req.setPublishPlanId(Lists.newArrayList(1L));
        assert publishPlanService.linkOrUnLinkPublishPlan(req).ifSuccess();
    }
}

































