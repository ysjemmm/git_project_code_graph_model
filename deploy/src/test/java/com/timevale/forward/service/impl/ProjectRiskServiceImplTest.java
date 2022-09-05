package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.query.ProjectRiskQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.service.component.HomePageRiskWarningComponent;
import com.timevale.forward.service.component.HomePageRiskWarningSubmitTestComponent;
import com.timevale.forward.service.component.HomePageRiskWarningTaskComponent;
import com.timevale.forward.service.component.ProjectRiskExplanationComponent;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectRiskServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectRiskServiceImpl projectRiskService;

    @Mock
    private ProjectRiskMapper projectRiskMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectRiskExplanationComponent projectRiskExplanationComponent;

    @Mock
    private HomePageRiskWarningComponent riskWarningComponent;

    @Mock
    private HomePageRiskWarningTaskComponent riskWarningTaskComponent;

    @Mock
    private HomePageRiskWarningSubmitTestComponent riskWarningSubmitTestComponent;

    @Test
    public void testAdd(){
        ProjectRiskAddReq req = new ProjectRiskAddReq();
        req.setProjectId(1L);
        req.setName("1");
        req.setExplanation("1");

        when(projectRiskMapper.insert(any())).thenReturn(1);
        doNothing().when(projectRiskExplanationComponent).add(any(),any());

        assert projectRiskService.add(req).ifSuccess();
    }

    @Test
    public void testModify(){
        ProjectRiskModifyReq req = new ProjectRiskModifyReq();
        req.setId(1L);
        req.setStatus(ProjectRiskStatusEnum.INVALID.getCode());
        req.setName("1");

        when(projectRiskMapper.update(any())).thenReturn(1);
        doNothing().when(projectRiskExplanationComponent).add(any(),any());

        assert  projectRiskService.modify(req).ifSuccess();
    }

    @Test
    public void testGet(){
        when(projectRiskMapper.selectById(any())).thenReturn(new ProjectRiskDO() {{
            setType(ProjectRiskTypeEnum.OTHER.getCode());
            setStatus(ProjectRiskStatusEnum.COMPLETE.getCode());
        }});

        assert  projectRiskService.get(1L).ifSuccess();
    }

    @Test
    public void testList(){
        ProjectRiskQueryList req = new ProjectRiskQueryList();
        req.setProjectId(1L);
        req.setPageNum(1);
        req.setPageSize(10);
        req.setStatusList(Lists.emptyList());

        List<ProjectRiskDO> riskDOList = new ArrayList<>();
        riskDOList.add(new ProjectRiskDO(){{
            setType(ProjectRiskTypeEnum.OTHER.getCode());
            setStatus(ProjectRiskStatusEnum.COMPLETE.getCode());
        }});
        when(projectRiskMapper.select(any())).thenReturn(riskDOList);

        assert projectRiskService.list(req).ifSuccess();
    }
}
