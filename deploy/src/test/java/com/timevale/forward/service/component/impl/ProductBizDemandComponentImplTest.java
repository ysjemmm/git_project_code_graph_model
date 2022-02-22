package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
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
 * @Date 2022/2/21 15:12
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProductBizDemandComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProductBizDemandComponentImpl productBizDemandComponent;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Test
    public void testUpdate() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setProductDemandId(1L);
        productBizDemandComponent.update(productBizDemandDO);
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testBatchInsert() {
        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setBizDemandId(1L);
        when(productBizDemandMapper.select(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        List<Long> list = new ArrayList<>();
        list.add(1L);
        list.add(2L);
        productBizDemandComponent.batchInsert(1L, list);
        localSessionUtilsMockedStatic.close();
    }
}






































