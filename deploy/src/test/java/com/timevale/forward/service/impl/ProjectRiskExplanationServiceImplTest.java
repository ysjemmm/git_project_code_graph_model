package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.ProjectRiskExplanationMapper;
import com.timevale.forward.facade.api.query.ProjectRiskExplanationQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskExplanationAddReq;
import com.timevale.forward.service.component.ProjectRiskExplanationComponent;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectRiskExplanationServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectRiskExplanationServiceImpl projectRiskExplanationService;

    @Mock
    private ProjectRiskExplanationComponent projectRiskExplanationComponent;

    @Mock
    private ProjectRiskExplanationMapper projectRiskExplanationMapper;

    @Test
    public void testAdd(){
        ProjectRiskExplanationAddReq req = new ProjectRiskExplanationAddReq();
        req.setExplanation("1");
        req.setProjectRiskId(1L);

        doNothing().when(projectRiskExplanationComponent).add(any(),any());

        assert  projectRiskExplanationService.add(req).ifSuccess();
    }

    @Test
    public void testList(){
        ProjectRiskExplanationQueryList req = new ProjectRiskExplanationQueryList();
        req.setPageNum(1);
        req.setPageSize(10);
        req.setProjectRiskId(1L);

        when(projectRiskExplanationMapper.selectByProjectRiskId(any())).thenReturn(Lists.emptyList());

        assert  projectRiskExplanationService.list(req).ifSuccess();
    }
}
