package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.dao.ProjectProductLineMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.ProjectProductLineDO;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/21 17:27
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectProductLineComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProjectProductLineComponentImpl projectProductLineComponent;

    @Mock
    private ProjectProductLineMapper projectProductLineMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private BugOfflineMapper bugOfflineMapper;
    @Test
    public void testAdd() {

        when(projectProductLineMapper.get(any())).thenReturn(null);

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        List<Long> list = new ArrayList<>();
        list.add(1L);
        projectProductLineComponent.add(list, 1L);
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testUpdate() {

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProjectProductLineDO projectProductLineDO = new ProjectProductLineDO();
        projectProductLineDO.setProductLineId(2L);
        when(projectProductLineMapper.get(any())).thenReturn(Collections.singletonList(projectProductLineDO));

        List<Long> list = new ArrayList<>();
        list.add(1L);
        projectProductLineComponent.update(list, 1L);
        localSessionUtilsMockedStatic.close();
    }

}




































