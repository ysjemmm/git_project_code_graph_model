package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/21 17:18
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectProductDemandComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProjectProductDemandComponentImpl projectProductDemandComponent;

    @Mock
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Mock
    private BizDemandComponent bizDemandComponent;

    @Mock
    private BizDemandLogComponent bizDemandLogComponent;

    @Mock
    private BizChangeLogMapper bizChangeLogMapper;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Test
    public void testUpdate() {
        when(productBizDemandMapper.getByProductDemandIds(any())).thenReturn(Lists.newArrayList(new ProductBizDemandDO(){{setBizDemandId(1L);}}));
        when(bizDemandComponent.getProjectEndDate(any())).thenReturn(new Date());
        when(bizDemandMapper.selectById(any())).thenReturn(new BizDemandDO(){{setProjectEndDate(new Date(1000L));}});
        projectProductDemandComponent.update(1L, 1L);
    }

    @Test
    public void testBatchInsert() {
        when(productBizDemandMapper.getByProductDemandIds(any())).thenReturn(Lists.newArrayList(new ProductBizDemandDO(){{setBizDemandId(1L);}}));
        when(bizDemandComponent.getProjectEndDate(any())).thenReturn(new Date());
        when(bizDemandMapper.selectById(any())).thenReturn(new BizDemandDO(){{setProjectEndDate(new Date(1000L));}});
        ProjectProductDemandDO projectProductDemandDO = new ProjectProductDemandDO();
        projectProductDemandDO.setProductDemandId(1L);
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Collections.singletonList(projectProductDemandDO));

        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        userInfo.setName("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        List<Long> productDemandIds = new ArrayList<>();
        productDemandIds.add(1L);
        productDemandIds.add(2L);
        projectProductDemandComponent.batchInsert(1L, productDemandIds);
        localSessionUtilsMockedStatic.close();
    }

}