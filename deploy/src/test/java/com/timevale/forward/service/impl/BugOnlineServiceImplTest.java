package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.BugOnlineAddReq;
import com.timevale.forward.facade.api.request.BugOnlineGetFieldReq;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.service.component.BugOnlineProductLineComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BugOnlineAddMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.mandarin.base.util.FieldUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Date;

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
        when(bugOnlineProductLineMapper.selectByBugOnlineIdList(any())).thenReturn(Lists.newArrayList(new BugOnlineProductLineDO() {{
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
        when(bugLogMapper.selectByBugOfflineIdAndType(any(), any(),any())).thenReturn(Lists.newArrayList(new BugLogDO()));
        FieldUtils.setFieldValue("defaultOperator", bugOnlineService, "1;2");

        MockedConstruction<BugOnlineAddMsgEvent> construction = mockConstruction(BugOnlineAddMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bugOnlineService.add(bugOnlineAddReq).getData();
        }finally {
            construction.close();
        }
    }
}

































