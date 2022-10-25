package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.JobFunctionEnum;
import com.timevale.forward.service.component.BugOnlineProductLineComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.FieldUtils;
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

import java.util.Date;
import java.util.HashSet;

import static org.mockito.Mockito.*;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BugOnlineServiceImplTest extends AbstractTestNGSpringContextTests {

    @Mock
    private BugOnlineMapper bugOnlineMapper;

    @Mock
    private BugOnlineProductLineMapper bugOnlineProductLineMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private BizDomainMapper bizDomainMapper;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private BugLogMapper bugLogMapper;

    @Mock
    private BugStatusOperatorMapper bugStatusOperatorMapper;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private FileComponent fileComponent;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private BugOnlineProductLineComponent bugOnlineProductLineComponent;

    @InjectMocks
    private BugOnlineServiceImpl bugOnlineService;

    @Test
    public void testGetAllDisplayField() {
        BugOnlineGetFieldReq req = new BugOnlineGetFieldReq();
        req.setProductLineIdList(Lists.newArrayList(1L));
        FieldUtils.setFieldValue("business", bugOnlineService, "[\n" +
                "    {\n" +
                "        \"fieldName\": \"mainOId\",\n" +
                "        \"fieldValue\": [\n" +
                "            1\n" +
                "        ]\n" +
                "    }\n" +
                "]");
        bugOnlineService.getAllDisplayField(req);
    }

    @Test
    public void testList() {

        BugOnlineQueryList bugOnlineQueryList = new BugOnlineQueryList();
        Date date = new Date();
        bugOnlineQueryList.setCreateDateLeft(date);
        bugOnlineQueryList.setCreateDateRight(date);
        bugOnlineQueryList.setModifyDateLeft(date);
        bugOnlineQueryList.setModifyDateRight(date);
        bugOnlineQueryList.setProposerIdList(Lists.newArrayList("1"));
        bugOnlineQueryList.setAscription("TEAM_SUBMIT");
        when(innerUserPersonClient.getAllMyStaffWithSelf(any(), any())).thenReturn(Lists.newArrayList("1"));
        when(bugOnlineMapper.selectListByCondition(any())).thenReturn(Lists.newArrayList(new BugOnlineListDO() {{
            setId(1L);
        }}));
        when(bugOnlineProductLineMapper.selectByBugOnlineIdList(any(),any())).thenReturn(Lists.newArrayList(new BugOnlineProductLineDO() {{
            setProductLineId(1L);
            setBugOnlineId(1L);
        }}));
        when(productLineMapper.selectByIds(any())).thenReturn(Lists.newArrayList(new ProductLineDO() {{
            setBizDomainId(1L);
            setName("1");
            setId(1L);
        }}));
        when(bizDomainMapper.selectByIdList(any())).thenReturn(Lists.newArrayList(new BizDomainDO() {{
            setId(1L);
        }}));
//        FieldUtils.setFieldValue("defaultOperator", bugOnlineService, "1");
        assert bugOnlineService.list(bugOnlineQueryList).ifSuccess();
    }

    @Test
    public void testAdd() {

        BugOnlineAddReq bugOnlineAddReq = new BugOnlineAddReq();
        bugOnlineAddReq.setName("1");
        bugOnlineAddReq.setSource("support");
        bugOnlineAddReq.setProductLineIdList(Lists.newArrayList(1L));
        bugOnlineAddReq.setFiles(Lists.newArrayList(new FileAddReq()));
        bugOnlineAddReq.setRecipients(Lists.newArrayList(new PersonAddReq()));
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        FieldUtils.setFieldValue("defaultOperator", bugOnlineService, "1;2");

        MockedConstruction<BugOnlineAddMsgEvent> construction = mockConstruction(BugOnlineAddMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOnlineService.add(bugOnlineAddReq).getData();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testDelete() {
        BugOnlineReq bugOnlineReq = new BugOnlineReq();
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        assert bugOnlineService.delete(bugOnlineReq).getData();
    }

    @Test
    public void testModify() {
        BugOnlineModifyReq bugOnlineModifyReq = new BugOnlineModifyReq();
        bugOnlineModifyReq.setProductLineIdList(Lists.newArrayList(1L));
        bugOnlineModifyReq.setFiles(Lists.newArrayList(new FileAddReq()));
        bugOnlineModifyReq.setRecipients(Lists.newArrayList(new PersonAddReq()));
        bugOnlineModifyReq.setBusiness("");
        bugOnlineModifyReq.setOperatorId("1");
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setOperatorId("2");
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        when(bugOnlineProductLineMapper.selectProductLineIds(any(),any())).thenReturn(Lists.newArrayList(2L));

        MockedConstruction<BugOnlineModifyMsgEvent> construction = mockConstruction(BugOnlineModifyMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
             bugOnlineService.modify(bugOnlineModifyReq).getData();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testGet() {
        BugOnlineDetailReq bugOnlineDetailReq = new BugOnlineDetailReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setOperatorId("2");
            setBizDemandId(1L);
            setDismissCause(1);
            setRepairFailReason("2");
            setReason(1);
        }});
        when(fileMapper.select(any(), any())).thenReturn(Lists.newArrayList(new FileDO()));
        when(personMapper.select(any())).thenReturn(Lists.newArrayList(new PersonDO()));
        when(commentMapper.select(any(), any())).thenReturn(Lists.newArrayList(new CommentDO()));
        when(bugOnlineProductLineMapper.selectProductLineIds(any(),any())).thenReturn(Lists.newArrayList(2L));

        bugOnlineService.get(bugOnlineDetailReq);
    }

    @Test
    public void testConfirm() {
        BugOnlineReq bugOnlineReq = new BugOnlineReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        bugOnlineService.confirm(bugOnlineReq);
    }

    @Test
    public void testStartRepair() {
        BugOnlineStartRepairReq repairReq = new BugOnlineStartRepairReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        bugOnlineService.startRepair(repairReq);
    }

    @Test
    public void testRepairFinished() {
        BugOnlineRepairFinishedReq finishedReq = new BugOnlineRepairFinishedReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.QUESTION_REPAIR.getCode());
            setRepairFailReason("1");
            setOperator("1");
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));

        MockedConstruction<BugOnlineRepairFinishedMsgEvent> construction = mockConstruction(BugOnlineRepairFinishedMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOnlineService.repairFinished(finishedReq).getData();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testConfirmRepair() {
        BugOnlineConfirmRepairReq confirmRepairReq = new BugOnlineConfirmRepairReq();
        confirmRepairReq.setReason(1);
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode());
            setRepairFailReason("1");
            setOperator("1");
        }});
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(new BaseInfoResponse(){{setJobFunction(JobFunctionEnum.QA.getName());}}));
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));

        assert bugOnlineService.confirmRepair(confirmRepairReq).getData();
    }

    @Test
    public void testOnline() {
        BugOnlineOnlineReq onlineOnlineReq = new BugOnlineOnlineReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.ONLINE.getCode());
            setReason(1);
            setOperator("1");
            setOperatorId("1");
            setProposerId("1");
        }});
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(new BaseInfoResponse(){{setJobFunction(JobFunctionEnum.QA.getName());}}));
        when(innerUserPersonClient.getAllSuperiorByAccount(any())).thenReturn(BaseResult.success(new HashSet<>()));
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));

        MockedConstruction<BugOnlineOnlineMsgEvent> construction = mockConstruction(BugOnlineOnlineMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOnlineService.online(onlineOnlineReq).getData();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testOpenAgain() {
        BugOnlineOpenAgainReq onlineOpenAgainReq = new BugOnlineOpenAgainReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.COMPLETE.getCode());
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));

        MockedConstruction<BugOnlineOpenAgainMsgEvent> construction = mockConstruction(BugOnlineOpenAgainMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOnlineService.openAgain(onlineOpenAgainReq).getData();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testNoRepair() {
        BugOnlineNoRepairReq noRepairReq = new BugOnlineNoRepairReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
            setRepairFailReason("2");
            setOperator("1");
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));

        MockedConstruction<BugOnlineNoRepairMsgEvent> construction = mockConstruction(BugOnlineNoRepairMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
             bugOnlineService.noRepair(noRepairReq).getData();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testTransfer() {
        BugOnlineTransferReq transferReq = new BugOnlineTransferReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode());
            setRepairFailReason("2");
            setOperator("1");
            setOperatorId("1");
            setProposerId("1");
        }});
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(new BaseInfoResponse(){{setJobFunction(JobFunctionEnum.QA.getName());}}));
        when(innerUserPersonClient.getAllSuperiorByAccount(any())).thenReturn(BaseResult.success(new HashSet<>()));
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));

        MockedConstruction<BugOnlineTransferMsgEvent> construction = mockConstruction(BugOnlineTransferMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOnlineService.transfer(transferReq).getData();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testAgree() {
        BugOnlineReq bugOnlineReq = new BugOnlineReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.BE_CONFIRM.getCode());
            setRepairFailReason("2");
            setOperator("1");
            setOperatorId("1");
            setProposerId("1");
        }});
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(new BaseInfoResponse(){{setJobFunction(JobFunctionEnum.QA.getName());}}));
        when(innerUserPersonClient.getAllSuperiorByAccount(any())).thenReturn(BaseResult.success(new HashSet<>()));
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        try {
            mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
            assert bugOnlineService.agree(bugOnlineReq).getData();
        }finally {
            mockStatic.close();
        }
    }

    @Test
    public void testReject() {
        BugOnlineReq bugOnlineReq = new BugOnlineReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.BE_CONFIRM.getCode());
            setRepairFailReason("2");
            setOperatorId("1");
            setProposerId("1");
            setOperator("1");
        }});
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(new BaseInfoResponse(){{setJobFunction(JobFunctionEnum.QA.getName());}}));
        when(innerUserPersonClient.getAllSuperiorByAccount(any())).thenReturn(BaseResult.success(new HashSet<>()));
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedConstruction<BugOnlineRejectMsgEvent> construction = mockConstruction(BugOnlineRejectMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        try {
            mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
            assert bugOnlineService.reject(bugOnlineReq).getData();
        }finally {
            mockStatic.close();
            construction.close();
        }
    }

    @Test
    public void testReconfirm() {
        BugOnlineReq bugOnlineReq = new BugOnlineReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
            setRepairFailReason("2");
            setOperatorId("1");
            setProposerId("1");
            setOperator("1");
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        assert bugOnlineService.reconfirm(bugOnlineReq).getData();
    }

    @Test
    public void testTemporaryNoRepair() {
        BugOnlineReq bugOnlineReq = new BugOnlineReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
            setRepairFailReason("2");
            setOperatorId("1");
            setProposerId("1");
            setOperator("1");
        }});
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        assert bugOnlineService.temporaryNoRepair(bugOnlineReq).getData();
    }

    @Test
    public void testRepairFailed() {
        BugOnlineRepairFailedReasonReq failedReasonReq = new BugOnlineRepairFailedReasonReq();
        when(bugOnlineMapper.selectById(any())).thenReturn(new BugOnlineDO() {{
            setStatus(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode());
            setRepairFailReason("2");
            setOperatorId("1");
            setProposerId("1");
            setOperator("1");
        }});
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(new BaseInfoResponse(){{setJobFunction(JobFunctionEnum.QA.getName());}}));
        when(innerUserPersonClient.getAllSuperiorByAccount(any())).thenReturn(BaseResult.success(new HashSet<>()));
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(), any())).thenReturn(Lists.newArrayList(new BugLogDO()));

        MockedConstruction<BugOnlineRepairFailedMsgEvent> construction = mockConstruction(BugOnlineRepairFailedMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        UserInfo userInfo = new UserInfo();
        userInfo.setId("1");
        MockedStatic<LocalSessionUtils> mockStatic = mockStatic(LocalSessionUtils.class);
        mockStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);
        try {
            assert bugOnlineService.repairFailed(failedReasonReq).getData();
        }finally {
            construction.close();
            mockStatic.close();
        }
    }
}

































