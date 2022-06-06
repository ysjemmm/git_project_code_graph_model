package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectPublishPlanMapper;
import com.timevale.forward.dal.dto.PublishPlanResultDTO;
import com.timevale.forward.dal.entity.ProjectPublishPlanDO;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectPublishPlanComponentImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectPublishPlanComponentImpl projectPublishPlanComponent;

    @Mock
    private ProjectPublishPlanMapper projectPublishPlanMapper;

    @Mock
    private PublishPlatformClient publishPlatformClient;

    @Test
    public void testAdd() {
        when(projectPublishPlanMapper.get(any())).thenReturn(Lists.newArrayList(new ProjectPublishPlanDO(){{setPublishPlanId(2L);}}));
        when(projectPublishPlanMapper.batchInsert(any())).thenReturn(1);
        when(projectPublishPlanMapper.update(any())).thenReturn(1);
        projectPublishPlanComponent.add(Lists.newArrayList(1L),1L);
    }

    @Test
    public void testUpdate() {
        projectPublishPlanComponent.update(null,null);
    }

    @Test
    public void testAnyMatchNotFinished() {
        when(projectPublishPlanMapper.get(any())).thenReturn(Lists.newArrayList(new ProjectPublishPlanDO(){{setPublishPlanId(2L);}}));
        when(publishPlatformClient.list(any())).thenReturn(new PublishPlanResultDTO(){{setList(new ArrayList<>());}});
        projectPublishPlanComponent.anyMatchNotFinished(any());
    }

    @Test
    public void testLinkPublishPlan() {
        when(projectPublishPlanMapper.get(any())).thenReturn(Lists.newArrayList(new ProjectPublishPlanDO(){{setPublishPlanId(2L);}}));
        projectPublishPlanComponent.linkPublishPlan(any());
    }
}

































