package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.BugLogQueryList;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugOfflineVO;
import com.timevale.forward.model.enums.BugStatusEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BugOfflineServiceImplTest extends AbstractTestNGSpringContextTests {
    @Mock
    private FileComponent fileComponent;

    @Mock
    MessageEventPublisher messageEventPublisher;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private BugOfflineMapper bugOfflineMapper;

    @Mock
    private BugLogMapper bugLogMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private BizDomainMapper bizDomainMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private BugStatusOperatorMapper bugStatusOperatorMapper;

    @InjectMocks
    private BugOfflineServiceImpl bugOfflineService;

    @Test
    public void testList() {
        BugOfflineQueryList bugOfflineQueryList = new BugOfflineQueryList();
        bugOfflineQueryList.setPageNum(1);
        bugOfflineQueryList.setPageSize(1);
        bugOfflineQueryList.setProposerIds(Lists.newArrayList("1"));
        bugOfflineQueryList.setOperatorIds(Lists.newArrayList("1"));

        bugOfflineQueryList.setAscription("TEAM_SUBMIT");
        when(innerUserPersonClient.getAllMyStaffWithSelf(any(), any())).thenReturn(Lists.newArrayList("1"));
        when(bugOfflineMapper.selectByCondition(any())).thenReturn(Lists.newArrayList(new BugOfflineListDO()));
        BaseResult<PageQueryResult<BugOfflineVO>> result = bugOfflineService.list(bugOfflineQueryList);

        bugOfflineQueryList.setAscription("TEAM_RECEIVE");
        assert bugOfflineService.list(bugOfflineQueryList).ifSuccess();
    }

    @Test
    public void testAdd() {
        BugOfflineAddReq bugOfflineAddReq = new BugOfflineAddReq();
        bugOfflineAddReq.setName("1");
        bugOfflineAddReq.setProjectId(1L);
        when(projectMapper.get(any())).thenReturn(new ProjectDO());
        MockedConstruction<BugOfflineAddMsg> construction = mockConstruction(BugOfflineAddMsg.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOfflineService.add(bugOfflineAddReq).ifSuccess();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testModify() {
        BugOfflineModifyReq bugOfflineModifyReq = new BugOfflineModifyReq();
        bugOfflineModifyReq.setProductLineId(2L);
        bugOfflineModifyReq.setProjectId(2L);
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProductLineId(1L);
            setProjectId(1L);
        }});
        when(projectMapper.getByIds(any())).thenReturn(new ArrayList<>());
        when(productLineMapper.selectByIds(any())).thenReturn(new ArrayList<>());
        MockedConstruction<BugOfflineUpdateMsg> construction = mockConstruction(BugOfflineUpdateMsg.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOfflineService.modify(bugOfflineModifyReq).ifSuccess();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testTransfer() {
        BugOfflineTransferReq transferReq = new BugOfflineTransferReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
        }});
        MockedConstruction<BugOfflineTransMsgEvent> construction = mockConstruction(BugOfflineTransMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.transfer(transferReq).ifSuccess();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }

    @Test
    public void testUnHandle() {
        BugOfflineUnHandleReq handleReq = new BugOfflineUnHandleReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.OPEN.getCode());
        }});
        MockedConstruction<BugOfflineNoRepairMsgEvent> construction = mockConstruction(BugOfflineNoRepairMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.unHandle(handleReq).ifSuccess();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }

    @Test
    public void testAgree() {
        BugOfflineReq handleReq = new BugOfflineReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.CONFIRM.getCode());
        }});

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.agree(handleReq).ifSuccess();
        }finally {
            mockStatic.close();
        }
    }

    @Test
    public void testReject() {
        BugOfflineReq bugOfflineReq = new BugOfflineReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.CONFIRM.getCode());
        }});
        MockedConstruction<BugOfflineRejectMsgEvent> construction = mockConstruction(BugOfflineRejectMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.reject(bugOfflineReq).ifSuccess();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }

    @Test
    public void testDelayHandle() {
        BugOfflineDelayHandleReq delayHandleReq = new BugOfflineDelayHandleReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.OPEN.getCode());
        }});
        MockedConstruction<BugOfflineDelayRepairMsgEvent> construction = mockConstruction(BugOfflineDelayRepairMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.delayHandle(delayHandleReq).ifSuccess();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }

    @Test
    public void testDoHandle() {
        BugOfflineReq bugOfflineReq = new BugOfflineReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.OPEN.getCode());
        }});

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        assert bugOfflineService.doHandle(bugOfflineReq).ifSuccess();
        try {

        }finally {
            mockStatic.close();
        }
    }

    @Test
    public void testPassSelf() {
        BugOfflinePassSelfReq bugOfflinePassSelfReq = new BugOfflinePassSelfReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.REPAIR.getCode());
            setCause("1");
            setSolvePlan("1");
        }});
        MockedConstruction<BugOfflineSelfTestPassMsgEvent> construction = mockConstruction(BugOfflineSelfTestPassMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.passSelf(bugOfflinePassSelfReq).ifSuccess();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }

    @Test
    public void testAccepted() {
        BugOfflineReq bugOfflineReq = new BugOfflineReq();

        BugOfflineDO bugOfflineDO = new BugOfflineDO();
        bugOfflineDO.setProposerId("1");
        bugOfflineDO.setOperatorId("1");
        bugOfflineDO.setStatus(BugStatusEnum.ACCEPTANCE.getCode());
        bugOfflineDO.setCause("1");
        bugOfflineDO.setSolvePlan("1");
        when(bugOfflineMapper.selectById(any())).thenReturn(bugOfflineDO);

        MockedConstruction<BugOfflineSelfTestPassMsgEvent> construction = mockConstruction(BugOfflineSelfTestPassMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        BaseResult<Set<String>> baseResult = new BaseResult<>();
        HashSet<String> accounts = new HashSet<>();
        accounts.add("SYSTEM");
        baseResult.setData(accounts);

        when(innerUserPersonClient.getAllSuperiorByAccount(any())).thenReturn(baseResult);

        try {
            assert bugOfflineService.accepted(bugOfflineReq).ifSuccess();
        }finally {
            construction.close();
        }
    }

    @Test
    public void testAcceptFailed() {
        BugOfflineReq bugOfflineReq = new BugOfflineReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.ACCEPTANCE.getCode());
            setReturnCount(1);
        }});
        MockedConstruction<BugOfflineCheckFailMsgEvent> construction = mockConstruction(BugOfflineCheckFailMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.acceptFailed(bugOfflineReq).ifSuccess();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }

    @Test
    public void testReopen() {
        BugOfflineReq bugOfflineReq = new BugOfflineReq();
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO(){{
            setProposerId("1");
            setOperatorId("1");
            setStatus(BugStatusEnum.COMPLETE.getCode());
            setReturnCount(1);
            setOpenCount(1);
            setUnhandleReason(1);
            setDelayHandleReason("1");
        }});
        MockedConstruction<BugOfflineOpenAgainMsgEvent> construction = mockConstruction(BugOfflineOpenAgainMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.reopen(bugOfflineReq).ifSuccess();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }

    @Test
    public void testGet() {
        when(bugOfflineMapper.selectById(any())).thenReturn(new BugOfflineDO());
        when(projectMapper.get(any())).thenReturn(new ProjectDO());
        when(productLineMapper.selectById(any())).thenReturn(new ProductLineDO());
        when(bizDomainMapper.selectById(any())).thenReturn(new BizDomainDO());
        when(fileMapper.select(any(),any())).thenReturn(new ArrayList<>());
        when(personMapper.select(any())).thenReturn(new ArrayList<>());
        when(commentMapper.select(any(),any())).thenReturn(new ArrayList<>());
        assert bugOfflineService.get(any()).ifSuccess();
    }

    @Test
    public void testDelete() {
        BugOfflineReq bugOfflineReq=new BugOfflineReq();
        assert bugOfflineService.delete(bugOfflineReq).ifSuccess();
    }

    @Test
    public void testBugLogList() {
        BugLogQueryList bugLogQueryList=new BugLogQueryList();
        bugLogQueryList.setStatusChange(true );
        when(bugLogMapper.selectByBugOfflineIdAndType(any(),any(),any())).thenReturn(Lists.newArrayList(new BugLogDO(){{setId(1L);}}));
        when(bugStatusOperatorMapper.batchSelectByBugIds(any())).thenReturn(Lists.newArrayList(new BugStatusOperatorDO(){{setBugLogId(1L);}}));
        assert bugOfflineService.bugLogList(bugLogQueryList).ifSuccess();
    }

}

































