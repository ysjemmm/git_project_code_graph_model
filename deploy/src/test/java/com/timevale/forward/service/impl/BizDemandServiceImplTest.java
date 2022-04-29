package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.observer.event.BizDemandInvalidMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandReceivedMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandRejectMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandToReceiveMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @Date 2022/2/17 14:45
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BizDemandServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private BizDemandServiceImpl bizDemandService;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private BizDomainMapper bizDomainMapper;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private FileComponent fileComponent;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Mock
    private BizDemandComponent bizDemandComponent;

    @Test
    public void testList() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);


        BaseResult<PageQueryResult<BizDemandVO>> baseResult = new BaseResult<>();
        baseResult.setMessage("成功");
        when(bizDemandComponent.page(any())).thenReturn(baseResult);

        BizDemandQueryList bizDemandQueryList = new BizDemandQueryList();
        bizDemandQueryList.setAscription("CURRENT_USER");
        bizDemandQueryList.setPageNum(1);
        bizDemandQueryList.setPageSize(5);
        PersonQuery personQuery = new PersonQuery();
        personQuery.setUserId("www");
        bizDemandQueryList.setBizDomainIdList(Collections.singletonList(1L));
        bizDemandQueryList.setCreateDateEnd(new Date());
        bizDemandQueryList.setCreateDateStart(new Date());

        assert bizDemandService.list(bizDemandQueryList).ifSuccess();

        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testUpdateStatus() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setId(1L);
        bizDemandDO.setReceiveManId("www");
        bizDemandDO.setName("轩振营");
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        when(bizDemandMapper.update(any())).thenReturn(1);

        when(productBizDemandMapper.deleteByBizDemandId(any())).thenReturn(1);

        MockedConstruction<BizDemandInvalidMsgEvent> bizDemandInvalidMsgEventMocked = mockConstruction(BizDemandInvalidMsgEvent.class);
        bizDemandInvalidMsgEventMocked.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        BizDemandUpdateStatusReq bizDemandUpdateStatusReq = new BizDemandUpdateStatusReq();
        bizDemandUpdateStatusReq.setBizDemandId(1L);
        assert bizDemandService.updateStatus(bizDemandUpdateStatusReq).ifSuccess();

        bizDemandInvalidMsgEventMocked.close();
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testAdd() {

        when(bizDemandMapper.selectByName(any())).thenReturn(null);

        when(bizDemandMapper.insert(any())).thenReturn(1);

        doNothing().when(fileComponent).add(any(), any(), any());

        doNothing().when(personComponent).add(any(), any(), any());

        MockedConstruction<BizDemandToReceiveMsgEvent> bizDemandToReceiveMsgEventMock = mockConstruction(BizDemandToReceiveMsgEvent.class);
        bizDemandToReceiveMsgEventMock.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        BizDemandAddReq bizDemandAddReq = new BizDemandAddReq();
        FileAddReq fileAddReq = new FileAddReq();
        fileAddReq.setFileId("www");
        bizDemandAddReq.setFileList(Collections.singletonList(fileAddReq));
        PersonAddReq personAddReq = new PersonAddReq();
        personAddReq.setUserId("www");
        bizDemandAddReq.setRecipientInfoList(Collections.singletonList(personAddReq));

        assert bizDemandService.add(bizDemandAddReq).ifSuccess();
        bizDemandToReceiveMsgEventMock.close();
    }

    @Test
    public void testGetBizDemandById() {

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setProductLineId(1L);
        bizDemandDO.setDeptId(1L);
        bizDemandDO.setReason(1);
        bizDemandDO.setStatus(1);
        bizDemandDO.setPriority(1);
        bizDemandDO.setPlanReleaseDate(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        FileDO fileDO = new FileDO();
        fileDO.setFileId("www");
        when(fileComponent.select(any(), any())).thenReturn(Collections.singletonList(fileDO));

        PersonDO personDO = new PersonDO();
        personDO.setUserId("www");
        when(personComponent.select(any(), any())).thenReturn(Collections.singletonList(personDO));

        ProductLineDO productLineDO = new ProductLineDO();
        productLineDO.setBizDomainId(1L);
        productLineDO.setName("www");
        when(productLineMapper.selectById(any())).thenReturn(productLineDO);

        BizDomainDO bizDomainDO = new BizDomainDO();
        when(bizDomainMapper.selectById(any())).thenReturn(bizDomainDO);

        GroupResponse groupResponse = new GroupResponse();
        groupResponse.setGroupName("www");
        groupResponse.setDeleteFlag(1);
        Map<Long, GroupResponse> map = new HashMap<>();
        map.put(1L, groupResponse);
        when(bizDemandComponent.getGroupListTreeMap(any())).thenReturn(map);

        when(bizDemandComponent.getProjectEndDate(any())).thenReturn(new Date());

        assert bizDemandService.getBizDemandById(1L).ifSuccess();
    }

    @Test
    public void testModify() {

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setStatus(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        when(bizDemandMapper.selectByName(any())).thenReturn(null);

        when(bizDemandMapper.update(any())).thenReturn(1);

        doNothing().when(personComponent).update(any(), any(), any());

        doNothing().when(fileComponent).update(any(), any(), any());

        BizDemandModifyReq bizDemandModifyReq = new BizDemandModifyReq();
        bizDemandModifyReq.setId(1L);
        bizDemandModifyReq.setName("www");
        PersonAddReq personAddReq = new PersonAddReq();
        personAddReq.setUserId("www");
        bizDemandModifyReq.setRecipientInfoList(Collections.singletonList(personAddReq));
        FileAddReq fileAddReq = new FileAddReq();
        fileAddReq.setFileId("www");
        bizDemandModifyReq.setFileList(Collections.singletonList(fileAddReq));

        assert bizDemandService.modify(bizDemandModifyReq).ifSuccess();
    }

    @Test
    public void testAgree() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setStatus(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        when(bizDemandMapper.update(any())).thenReturn(1);

        MockedConstruction<BizDemandReceivedMsgEvent> bizDemandReceivedMsgEventMock = mockConstruction(BizDemandReceivedMsgEvent.class);
        bizDemandReceivedMsgEventMock.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        BizDemandAgreeReq bizDemandAgreeReq = new BizDemandAgreeReq();
        bizDemandAgreeReq.setBizDemandId(1L);
        bizDemandAgreeReq.setPlanReleaseDate(1);
        assert bizDemandService.agree(bizDemandAgreeReq).ifSuccess();

        bizDemandReceivedMsgEventMock.close();
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testReject() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setStatus(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        BizDemandRejectReq bizDemandRejectReq = new BizDemandRejectReq();
        bizDemandRejectReq.setBizDemandId(1L);
        bizDemandRejectReq.setReason(1);

        when(bizDemandMapper.update(any())).thenReturn(1);

        MockedConstruction<BizDemandRejectMsgEvent> bizDemandRejectMsgEventMock = mockConstruction(BizDemandRejectMsgEvent.class);
        bizDemandRejectMsgEventMock.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        assert bizDemandService.reject(bizDemandRejectReq).ifSuccess();

        bizDemandRejectMsgEventMock.close();
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testTransfer() {

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setStatus(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        when(bizDemandMapper.update(any())).thenReturn(1);

        MockedConstruction<BizDemandToReceiveMsgEvent> MockedConstruction = mockConstruction(BizDemandToReceiveMsgEvent.class);
        MockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        BizDemandTransferReq bizDemandTransferReq = new BizDemandTransferReq();
        bizDemandTransferReq.setId(1L);
        bizDemandTransferReq.setReceiveMan("www");
        bizDemandTransferReq.setReceiveManId("www");

        assert bizDemandService.transfer(bizDemandTransferReq).ifSuccess();
        MockedConstruction.close();
    }

}








































