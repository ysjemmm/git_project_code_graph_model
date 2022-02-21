package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.facade.api.request.FileAddReq;
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
 * @Date 2022/2/21 13:40
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class FileComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private FileComponentImpl fileComponent;

    @Mock
    private FileMapper fileMapper;

    @Test
    public void testAdd() {
        when(fileMapper.select(any(), any())).thenReturn(null);

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        FileAddReq fileAddReq = new FileAddReq();
        fileAddReq.setFileId("www");
        List<FileAddReq> list = new ArrayList<>();
        list.add(fileAddReq);
        fileComponent.add(list, 1L, 1);
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testUpdate() {

        FileDO fileDO = new FileDO();
        fileDO.setFileId("www");
        when(fileMapper.select(any(), any())).thenReturn(Collections.singletonList(fileDO));

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        FileAddReq fileAddReq = new FileAddReq();
        fileAddReq.setFileId("www");
        List<FileAddReq> list = new ArrayList<>();
        list.add(fileAddReq);
        fileComponent.update(list, 1L, 1);

        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testSelect() {
        fileComponent.select(1L, 1);
    }
}






































