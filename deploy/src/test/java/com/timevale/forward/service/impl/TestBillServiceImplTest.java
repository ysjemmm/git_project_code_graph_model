package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.TestBillAddReq;
import com.timevale.forward.facade.api.request.TestBillModifyReq;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * @Date 2022/2/16 14:52
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class TestBillServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private TestBillServiceImpl testBillServiceImpl;

    @Mock
    private TestBillMapper testBillMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private ProjectNodeMapper projectNodeMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Mock
    private FileComponent fileComponent;

    @Test
    public void testAddTestBill() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProjectNodeDO projectNodeDO = new ProjectNodeDO();
        projectNodeDO.setName("提测");
        projectNodeDO.setPlanDate(new Date());
        when(projectNodeMapper.get(any())).thenReturn(Collections.singletonList(projectNodeDO));

        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setCreateManId("wangxuan");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        assert testBillServiceImpl.addTestBill(3L).ifSuccess();
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testSubmitTestBill() {

        when(testBillMapper.selectByProjectId(any())).thenReturn(null);

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName("www");
        when(projectMapper.get(any())).thenReturn(projectDO);

        MockedConstruction<BillTestCreateMsgEvent> billTestCreateMsgEventMockedConstruction = mockConstruction(BillTestCreateMsgEvent.class);
        billTestCreateMsgEventMockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        TestBillAddReq testBillAddReq = new TestBillAddReq();
        testBillAddReq.setTestManId("www");
        assert testBillServiceImpl.submitTestBill(testBillAddReq).ifSuccess();
        localSessionUtilsMockedStatic.close();
        billTestCreateMsgEventMockedConstruction.close();
    }

    @Test
    public void testGetTestBill() {
        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setProgress(1);
        testBillDO.setStatus(1);
        testBillDO.setCreateMan("www");
        testBillDO.setCreateManId("www");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName("www");
        projectDO.setPmName("www");
        when(projectMapper.get(any())).thenReturn(projectDO);

        FileDO fileDO = new FileDO();
        fileDO.setFileId("www");
        fileDO.setFileName("www");
        when(fileMapper.select(any(), any())).thenReturn(Collections.singletonList(fileDO));

        ProjectNodeDO projectNodeDO = new ProjectNodeDO();
        projectNodeDO.setName("提测");
        projectNodeDO.setActualDate(new Date());
        projectNodeDO.setPlanDate(new Date());
        when(projectNodeMapper.get(any())).thenReturn(Collections.singletonList(projectNodeDO));

        testBillServiceImpl.getTestBill(1L).ifSuccess();
    }

    @Test
    public void testSubmitSmokeTesting() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setCreateManId("www");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName("www");
        when(projectMapper.get(any())).thenReturn(projectDO);

        MockedConstruction<BillTestSubmitSmokeMsgEvent> billTestSubmitSmokeMsgEventMockedConstruction = mockConstruction(BillTestSubmitSmokeMsgEvent.class);
        billTestSubmitSmokeMsgEventMockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        TestBillModifyReq testBillModifyReq = new TestBillModifyReq();
        testBillModifyReq.setProjectId(1L);
        FileAddReq fileAddReq = new FileAddReq();
        fileAddReq.setFileId("www");
        List<FileAddReq> list = new ArrayList<>();
        list.add(fileAddReq);
        testBillModifyReq.setList(list);

        assert testBillServiceImpl.submitSmokeTesting(testBillModifyReq).ifSuccess();
        localSessionUtilsMockedStatic.close();
        billTestSubmitSmokeMsgEventMockedConstruction.close();
    }

    @Test
    public void testModifyTestMan() {

        ProjectDO projectDO = new ProjectDO();
        projectDO.setPmId("www");
        projectDO.setName("www");
        when(projectMapper.get(any())).thenReturn(projectDO);

        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setCreateManId("www");
        testBillDO.setTestManId("www");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        TestBillModifyReq testBillModifyReq = new TestBillModifyReq();
        testBillModifyReq.setTestManId("www");

        UserInfo userInfo = new UserInfo();
        userInfo.setId("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        when(projectMapper.get(any())).thenReturn(projectDO);

        MockedConstruction<BillTestModifyTestManMsgEvent> billTestModifyTestManMsgEventMockedConstruction = mockConstruction(BillTestModifyTestManMsgEvent.class);
        billTestModifyTestManMsgEventMockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        assert testBillServiceImpl.modifyTestMan(testBillModifyReq).ifSuccess();
        localSessionUtilsMockedStatic.close();
        billTestModifyTestManMsgEventMockedConstruction.close();
    }

    @Test
    public void testSelfTestPass() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProjectDO projectDO = new ProjectDO();
        projectDO.setName("www");
        when(projectMapper.get(any())).thenReturn(projectDO);

        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setTestManId("www");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        MockedConstruction<BillTestSelfTestPassMsgEvent> billTestSelfTestPassMsgEventMockedConstruction = mockConstruction(BillTestSelfTestPassMsgEvent.class);
        billTestSelfTestPassMsgEventMockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        TestBillModifyReq testBillModifyReq = new TestBillModifyReq();
        testBillModifyReq.setProjectId(1L);
        FileAddReq fileAddReq = new FileAddReq();
        fileAddReq.setFileId("www");
        List<FileAddReq> list = new ArrayList<>();
        list.add(fileAddReq);
        testBillModifyReq.setList(list);
        assert testBillServiceImpl.selfTestPass(testBillModifyReq).ifSuccess();
        localSessionUtilsMockedStatic.close();
        billTestSelfTestPassMsgEventMockedConstruction.close();
    }

    @Test
    public void testSubmitTestPass() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setName("www");
        when(projectMapper.get(any())).thenReturn(projectDO);

        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setCreateManId("www");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        MockedConstruction<BillTestSubmitTestSuccessMsgEvent> billTestSubmitTestSuccessMsgEventMockedConstruction = mockConstruction(BillTestSubmitTestSuccessMsgEvent.class);
        billTestSubmitTestSuccessMsgEventMockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        TestBillModifyReq testBillModifyReq = new TestBillModifyReq();
        testBillModifyReq.setProjectId(1L);
        assert testBillServiceImpl.submitTestPass(testBillModifyReq).ifSuccess();
        billTestSubmitTestSuccessMsgEventMockedConstruction.close();
    }

    @Test
    public void testSubmitTestBack() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setName("www");
        when(projectMapper.get(any())).thenReturn(projectDO);

        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setCreateManId("www");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        MockedConstruction<BillTestSubmitTestFailMsgEvent> billTestSubmitTestFailMsgEventMockedConstruction = mockConstruction(BillTestSubmitTestFailMsgEvent.class);
        billTestSubmitTestFailMsgEventMockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        TestBillModifyReq testBillModifyReq = new TestBillModifyReq();
        testBillModifyReq.setProjectId(1L);
        assert testBillServiceImpl.submitTestBack(testBillModifyReq).ifSuccess();
        billTestSubmitTestFailMsgEventMockedConstruction.close();
    }
}





























