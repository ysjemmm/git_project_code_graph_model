package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.dal.entity.ProjectProductLineBizDomain;
import com.timevale.forward.service.utils.ResultUtil;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

    @Test
    public void testPage() {
        when(personMapper.getMainIds(any(), any(), any())).thenReturn(Arrays.asList(1L, 2L));

        when(personMapper.getMainIds(any(), any(), any())).thenReturn(Arrays.asList(1L, 2L));

        when(projectMapper.getProjectIds(any(), any(), any())).thenReturn(Arrays.asList(1L, 2L));

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
        assert projectComponent.page(projectListCondition, projectIds,false).ifSuccess();
    }
}

































