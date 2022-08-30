package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.model.enums.TestBillStatusEnum;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.utils.ResultUtil;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/21 16:31
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProjectComponentImpl projectComponent;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private ProjectRiskMapper projectRiskMapper;

    @Mock
    private ProjectNodeMapper projectNodeMapper;

    @Mock
    private ProjectNodeComponent projectNodeComponent;

    @Mock
    private TestBillMapper testBillMapper;

    @Mock
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Test
    public void testPage() {
        when(personMapper.getMainIds(any(), any(), any())).thenReturn(Arrays.asList(1L, 2L));

        when(personMapper.getMainIds(any(), any(), any())).thenReturn(Arrays.asList(1L, 2L));

        when(projectMapper.getProjectIds(any(), any(), any())).thenReturn(Arrays.asList(1L, 2L));

        when(testBillMapper.list(any())).thenReturn(Lists.newArrayList(new TestBillDO(){{setProjectId(1L);setDelayDay(1);}}));

        ProjectListDO projectListDO = new ProjectListDO();
        projectListDO.setId(1L);
        projectListDO.setType(1);
        projectListDO.setStatus(1);
        projectListDO.setPriority(1);
        when(projectMapper.list(any())).thenReturn(Collections.singletonList(projectListDO));

        PersonDO personDO = new PersonDO();
        personDO.setUserName("www");
        personDO.setMainId(1L);
        List<PersonDO> list = new ArrayList<>();
        list.add(personDO);

        when(personMapper.get(any(), any())).thenReturn(list);

        when(personMapper.get(any(), any())).thenReturn(list);

        ProjectProductLineBizDomain projectProductLineBizDomain = new ProjectProductLineBizDomain();
        projectProductLineBizDomain.setProductLineName("www");
        projectProductLineBizDomain.setBizDomainName("www");
        projectProductLineBizDomain.setProjectId(1L);
        when(productLineMapper.getByProjectIds(any())).thenReturn(Collections.singletonList(projectProductLineBizDomain));

        MockedStatic<ResultUtil> ResultUtilMockedStatic = mockStatic(ResultUtil.class);
        ResultUtilMockedStatic.close();

        ProjectListCondition projectListCondition = ProjectListCondition.builder()
                .pds(Collections.singletonList("www"))
                .teamMembers(Collections.singletonList("www"))
                .productLineIds(Collections.singletonList(1L))
                .build();
        List<Long> projectIds = new ArrayList<>();
        projectIds.add(1L);
//        assert projectComponent.page(projectListCondition, projectIds).getPageQueryResult().isSuccess();
    }

    @Test
    public void testUpdateEndDate() {
        ProjectNodeDO projectNodeDO=new ProjectNodeDO();
        projectNodeDO.setActualDate(new Date(200));
        projectNodeDO.setPlanDate(new Date(100));
        projectNodeDO.setName(ProjectNodeEnum.SUBMIT_TEST.getText());
        List<ProjectNodeDO> projectNodes=Lists.newArrayList(projectNodeDO);
        ProjectDO projectDO=new ProjectDO();
        when(projectNodeMapper.getByName(any(),any())).thenReturn(new ProjectNodeDO(){{}});
        when(testBillMapper.selectByProjectId(any())).thenReturn(new TestBillDO(){{
            setStatus(TestBillStatusEnum.TEST_SUCCESS.getCode());
        }});
         projectComponent.fillInfo(projectNodes, projectDO);
    }

    @Test
    public void testGetStatus() {
        ProjectNodeDO projectNodeDO=new ProjectNodeDO();
        projectNodeDO.setName(ProjectNodeEnum.SUBMIT_TEST.getText());
        List<ProjectNodeDO> projectNodes=Lists.newArrayList(projectNodeDO);
        when(projectNodeComponent.get(anyLong())).thenReturn(projectNodes);
        projectComponent.getStatus(1L);
    }

    @Test
    public void testUpdateNodeStatus() {
        ProjectNodeDO projectNodeDO=new ProjectNodeDO();
        projectNodeDO.setName(ProjectNodeEnum.SUBMIT_TEST.getText());
        List<ProjectNodeDO> projectNodes=Lists.newArrayList(projectNodeDO);
        when(projectNodeComponent.get(anyLong())).thenReturn(projectNodes);
        when(projectMapper.get(anyLong())).thenReturn(new ProjectDO());
        projectComponent.updateNodeStatus(1L);
    }

    @Test
    public void testGetLinkBizDemandIds() {
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Lists.newArrayList(new ProjectProductDemandDO(){{setProductDemandId(1L);}}));
        projectComponent.getLinkBizDemandIds(1L);
    }
}

































