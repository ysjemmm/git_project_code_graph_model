package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
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
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mockStatic;

/**
 * @Date 2022/2/21 17:07
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectNodeComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProjectNodeComponentImpl projectNodeComponent;

    @Mock
    private ProjectNodeMapper projectNodeMapper;

    @Test
    public void testAdd() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProjectNodeDO projectNodeDO = new ProjectNodeDO();
        projectNodeDO.setId(1L);
        List<ProjectNodeDO> list = new ArrayList<>();
        list.add(projectNodeDO);

        projectNodeComponent.add(list, 1L);
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testGet() {
        projectNodeComponent.get(1L);
    }

    @Test
    public void testBuildDefaultNode() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        projectNodeComponent.buildDefaultNode(new Date(), new Date(), 1L);
        localSessionUtilsMockedStatic.close();
    }
}






























