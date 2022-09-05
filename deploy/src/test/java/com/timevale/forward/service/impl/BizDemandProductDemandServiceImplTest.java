package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandProductDemandQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.request.BizDemandLinkProductDemandReq;
import com.timevale.forward.facade.api.request.BizDemandUnlinkProductDemandReq;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BizDemandPlanReleaseDateMsgEvent;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @Date 2022/2/17 11:06
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BizDemandProductDemandServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private BizDemandProductDemandServiceImpl bizDemandProductDemandService;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Mock
    private ProductDemandMapper productDemandMapper;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Mock
    private ProductDemandService productDemandService;

    @Mock
    private BizDemandComponent bizDemandComponent;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private BizDemandLogComponent bizDemandLogComponent;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Test
    public void testLinkedProductDemandList() {
        BizDemandLinkProductDemandListDO bizDemandLinkProductDemandListDO = new BizDemandLinkProductDemandListDO();
        bizDemandLinkProductDemandListDO.setPriority(1);
        bizDemandLinkProductDemandListDO.setStatus(1);
        when(productDemandMapper.selectByBizDemandId(any())).thenReturn(Collections.singletonList(bizDemandLinkProductDemandListDO));

        BizDemandProductDemandQueryList bizDemandProductDemandQueryList = new BizDemandProductDemandQueryList();
        bizDemandProductDemandQueryList.setPageNum(1);
        bizDemandProductDemandQueryList.setPageSize(5);

        assert bizDemandProductDemandService.linkedProductDemandList(bizDemandProductDemandQueryList).ifSuccess();
    }

    @Test
    public void testGetProductDemand() {
        ProductDemandDetailVO productDemandDetailVO = new ProductDemandDetailVO();
        productDemandDetailVO.setName("天龙八部");
        BaseResult<ProductDemandDetailVO> baseResult = new BaseResult<>();
        baseResult.setData(productDemandDetailVO);

        when(productDemandService.get(any())).thenReturn(baseResult);
        assert bizDemandProductDemandService.getProductDemand(1L).ifSuccess();
    }

    @Test
    public void testLinkProductDemand() {
        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setId(1L);
        bizDemandDO.setStatus(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setProductDemandId(1L);
        productBizDemandDO.setIsDeleted(true);
        when(productBizDemandMapper.select(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        when(productBizDemandMapper.inserts(any())).thenReturn(1);
        when(productBizDemandMapper.updates(any(), any())).thenReturn(1);
        doNothing().when(bizDemandComponent).updateBizDemandStatusByLinkedProductDemand(any());

        BizDemandDO bizDemand = new BizDemandDO();
        bizDemand.setStatus(1);
        bizDemand.setId(1L);
        bizDemand.setReceiveManId("www");
        bizDemand.setName("www");
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemand);

        Date date = new Date();
        when(bizDemandComponent.getProjectEndDate(any())).thenReturn(date);

        BizDemandLinkProductDemandReq bizDemandLinkProductDemandReq = new BizDemandLinkProductDemandReq();
        bizDemandLinkProductDemandReq.setId(1L);
        bizDemandLinkProductDemandReq.setProductDemandIdList(Arrays.asList(1L, 2L));

        doNothing().when(bizDemandLogComponent).addLogWhenModifyData(any(),any());

        MockedConstruction<BizDemandPlanReleaseDateMsgEvent> construction = mockConstruction(BizDemandPlanReleaseDateMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bizDemandProductDemandService.linkProductDemand(bizDemandLinkProductDemandReq).ifSuccess();
        } finally {
            construction.close();
        }


    }

    @Test
    public void testUnlinkProductDemand() {

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setId(1L);
        bizDemandDO.setStatus(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setId(1L);
        when(productBizDemandMapper.select(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        when(productBizDemandMapper.delete(any())).thenReturn(1);
        doNothing().when(bizDemandComponent).updateBizDemandStatusByLinkedProductDemand(any());

        BizDemandDO bizDemand = new BizDemandDO();
        bizDemand.setStatus(1);
        bizDemand.setId(1L);
        bizDemand.setReceiveManId("www");
        bizDemand.setName("www");
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemand);

        Date date = new Date();
        when(bizDemandComponent.getProjectEndDate(any())).thenReturn(date);

        BizDemandUnlinkProductDemandReq bizDemandUnlinkProductDemandReq = new BizDemandUnlinkProductDemandReq();
        bizDemandUnlinkProductDemandReq.setBizDemandId(1L);
        bizDemandUnlinkProductDemandReq.setProductDemandId(1L);

        MockedConstruction<BizDemandPlanReleaseDateMsgEvent> construction = mockConstruction(BizDemandPlanReleaseDateMsgEvent.class);
        construction.constructed();
        doNothing().when(messageEventPublisher).publish(any());
        try {
            assert bizDemandProductDemandService.unlinkProductDemand(bizDemandUnlinkProductDemandReq).ifSuccess();
        } finally {
            construction.close();
        }
    }

    @Test
    public void testMatchProductDemandList() {

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setProductDemandId(1L);
        when(productBizDemandMapper.select(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        when(innerUserPersonClient.getAllMyStaffWithSelf(any(), any())).thenReturn(Collections.singletonList("www"));

        BizDemandLinkProductDemandListDO bizDemandLinkProductDemandListDO = new BizDemandLinkProductDemandListDO();
        bizDemandLinkProductDemandListDO.setPriority(1);
        bizDemandLinkProductDemandListDO.setStatus(1);

        BizDemandLinkProductDemandQueryList bizDemandLinkProductDemandQueryList = new BizDemandLinkProductDemandQueryList();
        bizDemandLinkProductDemandQueryList.setPageNum(1);
        bizDemandLinkProductDemandQueryList.setPageSize(5);
        bizDemandLinkProductDemandQueryList.setCreateDateStart(new Date());
        bizDemandLinkProductDemandQueryList.setCreateDateEnd(new Date());
        PersonQuery personQuery = new PersonQuery();
        personQuery.setUserId("www");
        bizDemandLinkProductDemandQueryList.setOwnerInfoList(Collections.singletonList(personQuery));

        assert bizDemandProductDemandService.matchProductDemandList(bizDemandLinkProductDemandQueryList).ifSuccess();

        localSessionUtilsMockedStatic.close();
    }

}


























