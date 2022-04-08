package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.observer.event.BizDemandStatusChangeMsgEvent;
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
 * @Date 2022/2/21 15:50
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProductDemandComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProductDemandComponentImpl productDemandComponent;

    @Mock
    private ProductDemandMapper productDemandMapper;

    @Mock
    private FileComponent fileComponent;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Mock
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    private BizDemandComponent bizDemandComponent;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Test
    public void testList() {
        ProductDemandListCondition productDemandListCondition = ProductDemandListCondition.builder()
                .name("www")
                .createDateStart(new Date())
                .createDateEnd(new Date())
                .build();
        productDemandComponent.list(productDemandListCondition);
    }

    @Test
    public void testGet() {
        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setType("[1,2]");
        productDemandDO.setProductLineId(1L);
        productDemandDO.setStatus(1);
        productDemandDO.setPriority(1);
        when(productDemandMapper.get(any())).thenReturn(productDemandDO);

        ProductLineDO productLineDO = new ProductLineDO();
        productLineDO.setName("www");
        when(productLineMapper.selectById(any())).thenReturn(productLineDO);

        FileDO fileDO = new FileDO();
        fileDO.setFileId("www");
        when(fileComponent.select(any(), any())).thenReturn(Collections.singletonList(fileDO));

        PersonDO personDO = new PersonDO();
        personDO.setUserId("www");
        when(personComponent.select(any(), any())).thenReturn(Collections.singletonList(personDO));

        productDemandComponent.get(1L);
    }

    @Test
    public void testUpdate() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setPriority(1);
        productDemandComponent.update(productDemandDO);
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testUpdateProductDemandStatus() {
        ProjectProductDemandDO projectProductDemandDO = new ProjectProductDemandDO();
        projectProductDemandDO.setProductDemandId(1L);
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Collections.singletonList(projectProductDemandDO));

        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setBizDemandId(1L);
        productBizDemandDO.setStatus(1);
        when(productBizDemandMapper.getByProductDemandIds(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        when(productBizDemandMapper.getByBizDemandId(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        MockedConstruction<BizDemandStatusChangeMsgEvent> bizDemandStatusChangeMsgEventMock = mockConstruction(BizDemandStatusChangeMsgEvent.class);
        bizDemandStatusChangeMsgEventMock.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        productDemandComponent.updateProductDemandStatus(1L, 0);

        bizDemandStatusChangeMsgEventMock.close();
    }

    @Test
    public void testUpdateBizDemandStatusAsProductStatusChange() {
        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setBizDemandId(1L);
        productBizDemandDO.setStatus(1);
        when(productBizDemandMapper.getByProductDemandIds(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        MockedConstruction<BizDemandStatusChangeMsgEvent> bizDemandStatusChangeMsgEventMock = mockConstruction(BizDemandStatusChangeMsgEvent.class);
        bizDemandStatusChangeMsgEventMock.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        List<Long> list = new ArrayList<>();
        list.add(1L);
        productDemandComponent.updateBizDemandStatusAsProductStatusChange(list, false);

        bizDemandStatusChangeMsgEventMock.close();
    }
}







































