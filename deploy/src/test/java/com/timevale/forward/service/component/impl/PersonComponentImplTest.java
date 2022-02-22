package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
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
 * @Date 2022/2/21 13:56
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class PersonComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private PersonComponentImpl personComponent;

    @Mock
    private PersonMapper personMapper;

    @Test
    public void testAdd() {
        when(personMapper.select(any())).thenReturn(null);

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        PersonAddReq personAddReq = new PersonAddReq();
        personAddReq.setUserId("www");
        List<PersonAddReq> list = new ArrayList<>();
        list.add(personAddReq);
        personComponent.add(list, 1L, 1);
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

        PersonDO personDO = new PersonDO();
        personDO.setUserId("qqq");
        when(personMapper.select(any())).thenReturn(Collections.singletonList(personDO));

        PersonAddReq personAddReq = new PersonAddReq();
        personAddReq.setUserId("www");
        List<PersonAddReq> list = new ArrayList<>();
        list.add(personAddReq);
        personComponent.update(list, 1L, 1);
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testSelect() {
        PersonDO personDO = new PersonDO();
        personDO.setUserId("www");
        when(personMapper.select(any())).thenReturn(Collections.singletonList(personDO));

        personComponent.select(1L, 1);
    }

}




































