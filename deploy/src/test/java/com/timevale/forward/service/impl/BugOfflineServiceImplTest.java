package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.BugLogQueryList;
import com.timevale.forward.facade.api.query.BugOfflineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugOfflineVO;
import com.timevale.forward.model.enums.BugStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
            setProductLineId(1L);
            setProjectId(1L);
        }});
        when(projectMapper.getByIds(any())).thenReturn(new ArrayList<>());
        when(productLineMapper.getByIds(any())).thenReturn(new ArrayList<>());
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(bugOfflineDO);

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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO(){{
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
        when(bugOfflineMapper.get(any())).thenReturn(new BugOfflineDO());
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
    public void testChangeProjectAllowsCompletedBug() {
        BugOfflineChangeProjectReq req = new BugOfflineChangeProjectReq();
        req.setIds(Lists.newArrayList(1L));
        req.setProjectId(2L);
        req.setProductLineId(3L);

        BugOfflineDO bugOfflineDO = new BugOfflineDO();
        bugOfflineDO.setId(1L);
        bugOfflineDO.setName("bug-1");
        bugOfflineDO.setStatus(BugStatusEnum.COMPLETE.getCode());
        bugOfflineDO.setOperatorId("1");
        bugOfflineDO.setProposerId("2");
        bugOfflineDO.setProjectId(1L);
        bugOfflineDO.setProductLineId(1L);
        when(bugOfflineMapper.getByIdList(any())).thenReturn(Lists.newArrayList(bugOfflineDO));

        ProjectDO targetProjectDO = new ProjectDO();
        targetProjectDO.setId(2L);
        targetProjectDO.setName("target-project");
        targetProjectDO.setStatus(ProjectStatusEnum.DEVING.getCode());
        when(projectMapper.get(2L)).thenReturn(targetProjectDO);
        when(projectMapper.get(1L)).thenReturn(new ProjectDO() {{
            setId(1L);
            setName("source-project");
        }});

        when(productLineMapper.selectById(3L)).thenReturn(new ProductLineDO() {{
            setId(3L);
            setName("target-product-line");
        }});
        when(productLineMapper.selectById(1L)).thenReturn(new ProductLineDO() {{
            setId(1L);
            setName("source-product-line");
        }});
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(new BaseInfoResponse()));

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOfflineService.changeProject(req).ifSuccess();
            verify(bugOfflineMapper).updateProjectAndProductLine(req.getIds(), req.getProjectId(), req.getProductLineId());
            verify(bugLogMapper).batchInsert(any());
        } finally {
            mockStatic.close();
        }
    }

    @Test(expectedExceptions = BaseBizRuntimeException.class,
            expectedExceptionsMessageRegExp = "关闭状态的bug不能变更项目，请修改后重试")
    public void testChangeProjectRejectsClosedBug() {
        BugOfflineChangeProjectReq req = new BugOfflineChangeProjectReq();
        req.setIds(Lists.newArrayList(1L));
        req.setProjectId(2L);
        req.setProductLineId(3L);

        BugOfflineDO bugOfflineDO = new BugOfflineDO();
        bugOfflineDO.setId(1L);
        bugOfflineDO.setStatus(BugStatusEnum.CLOSE.getCode());
        when(bugOfflineMapper.getByIdList(any())).thenReturn(Lists.newArrayList(bugOfflineDO));

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            bugOfflineService.changeProject(req);
        } finally {
            mockStatic.close();
        }
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































