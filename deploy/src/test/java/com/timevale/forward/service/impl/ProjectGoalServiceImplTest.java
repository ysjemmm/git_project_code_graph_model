package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProjectGoalMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectGoalDO;
import com.timevale.forward.facade.api.request.ProjectGoalAddReq;
import com.timevale.forward.facade.api.request.ProjectGoalFinishReq;
import com.timevale.forward.facade.api.request.ProjectGoalModifyReq;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.security.facade.response.BaseInfoResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author jingchun
 * create on 2022/6/22
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectGoalServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectGoalServiceImpl projectGoalService;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectGoalMapper projectGoalMapper;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private InnerUserPersonClient innerUserPersonClient;
    @Mock
    private BizChangeLogMapper bizChangeLogMapper;

    @Test
    public void testList() {

        when(projectGoalMapper.getByProjectId(any()))
                .thenReturn(Lists.newArrayList(
                        new ProjectGoalDO(),
                        new ProjectGoalDO()
                ));
        assertNotEquals(projectGoalService.list(1L).getData().size(), 0);
    }

    @Test
    public void testAdd() {
        ProjectGoalAddReq req = new ProjectGoalAddReq();
        req.setProjectId(1L);
        ProjectDO project = new ProjectDO();
        project.setIsWithGoal(1);
        when(projectMapper.get(1L)).thenReturn(project);
        PersonDO person = new PersonDO();
        person.setUserId("test");
        when(personMapper.select(any())).thenReturn(Collections.singletonList(person));
        try (MockedStatic<LocalSessionUtils> mocked = mockStatic(LocalSessionUtils.class)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId("testSup");
            mocked.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
            when(innerUserPersonClient.getAllSuperiorByAccount("test", false))
                    .thenReturn(Collections.singleton("testSup"));
            assertTrue(projectGoalService.add(req).getData());
        }
    }

    @Test
    public void testModify() {
        ProjectGoalModifyReq req = new ProjectGoalModifyReq();
        req.setId(1L);
        ProjectGoalDO projectGoal = new ProjectGoalDO();
        projectGoal.setId(1L);
        projectGoal.setStatus(0);
        projectGoal.setProjectId(1L);
        when(projectGoalMapper.get(1L)).thenReturn(projectGoal);
        when(projectMapper.get(1L)).thenReturn(new ProjectDO());
        PersonDO person = new PersonDO();
        person.setUserId("test");
        when(personMapper.select(any())).thenReturn(Collections.singletonList(person));
        try (MockedStatic<LocalSessionUtils> mocked = mockStatic(LocalSessionUtils.class)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId("test");
            mocked.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
            assertTrue(projectGoalService.modify(req).getData());
        }
    }

    @Test
    public void testDelete() {
        ProjectGoalModifyReq req = new ProjectGoalModifyReq();
        req.setId(1L);
        ProjectGoalDO projectGoal = new ProjectGoalDO();
        projectGoal.setId(1L);
        projectGoal.setStatus(0);
        projectGoal.setProjectId(1L);
        when(projectGoalMapper.get(1L)).thenReturn(projectGoal);
        ProjectDO project = new ProjectDO();
        project.setId(1L);
        when(projectMapper.get(1L)).thenReturn(project);
        ProjectGoalDO mockedDO2 = new ProjectGoalDO();
        mockedDO2.setId(2L);
        when(projectGoalMapper.getByProjectId(eq(1L))).thenReturn(Lists.newArrayList(
                projectGoal, mockedDO2
        ));
        PersonDO person = new PersonDO();
        person.setUserId("test");
        when(personMapper.select(any())).thenReturn(Collections.singletonList(person));
        try (MockedStatic<LocalSessionUtils> mocked = mockStatic(LocalSessionUtils.class)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId("test");
            mocked.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
            assertTrue(projectGoalService.delete(1L).getData());
            projectGoal.setIsMain(1);
            assertTrue(projectGoalService.delete(1L).getData());
        }
    }

    @Test
    public void testSetMain() {
        ProjectGoalDO projectGoal = new ProjectGoalDO();
        projectGoal.setId(1L);
        projectGoal.setStatus(0);
        projectGoal.setProjectId(1L);
        projectGoal.setIsMain(0);
        when(projectGoalMapper.get(1L)).thenReturn(projectGoal);
        ProjectDO project = new ProjectDO();
        project.setId(1L);
        when(projectMapper.get(1L)).thenReturn(project);
        PersonDO person = new PersonDO();
        person.setUserId("test");
        when(personMapper.select(any())).thenReturn(Collections.singletonList(person));
        try (MockedStatic<LocalSessionUtils> mocked = mockStatic(LocalSessionUtils.class)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId("test");
            mocked.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
            assertTrue(projectGoalService.setMainGoal(1L).getData());
        }
    }

    @Test
    public void testFinish() {
        ProjectGoalDO projectGoal = new ProjectGoalDO();
        projectGoal.setId(1L);
        projectGoal.setStatus(0);
        projectGoal.setProjectId(1L);
        projectGoal.setIsMain(0);
        when(projectGoalMapper.get(1L)).thenReturn(projectGoal);
        ProjectDO project = new ProjectDO();
        project.setId(1L);
        when(projectMapper.get(1L)).thenReturn(project);
        PersonDO person = new PersonDO();
        person.setUserId("test");
        BaseInfoResponse res = new BaseInfoResponse();
        res.setJob("PMO");
        res.setAccount("test");
        when(innerUserPersonClient.getAllMyStaffWithSelfInfo(any(), eq(false)))
                .thenReturn(Lists.newArrayList(res));
        try (MockedStatic<LocalSessionUtils> mocked = mockStatic(LocalSessionUtils.class)) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId("test");
            mocked.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
            ProjectGoalFinishReq req = new ProjectGoalFinishReq();
            req.setId(1L);
            req.setStatus(10);
            req.setCompleteNote("yeah");
            assertTrue(projectGoalService.finish(req).getData());
        }
    }


}
