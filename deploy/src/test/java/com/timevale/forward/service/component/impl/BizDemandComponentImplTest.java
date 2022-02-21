package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.security.facade.response.GroupResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/21 9:56
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BizDemandComponentImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private BizDemandComponentImpl bizDemandComponent;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Mock
    private ProductDemandMapper productDemandMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Mock
    private InnerGroupClient innerGroupClient;

    @Test
    public void testUpdateBizDemandStatusByLinkedProductDemand() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("www");
        userInfo.setId("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setProductDemandId(1L);
        when(productBizDemandMapper.getByBizDemandId(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setStatus(10);
        when(productDemandMapper.selectByIdList(any())).thenReturn(Collections.singletonList(productDemandDO));

        BizDemandDO bizDemandDO = new BizDemandDO();
        bizDemandDO.setStatus(1);
        when(bizDemandMapper.selectById(any())).thenReturn(bizDemandDO);

        bizDemandComponent.updateBizDemandStatusByLinkedProductDemand(1L);
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testGetGroupListTreeMap() {
        GroupResponse groupResponse = new GroupResponse();
        GroupResponse childNode = new GroupResponse();
        childNode.setGroupId("1");
        childNode.setGroupName("www");
        GroupResponse grandSonNode = new GroupResponse();
        grandSonNode.setGroupId("1");
        grandSonNode.setGroupName("www");
        childNode.setChildNode(Collections.singletonList(grandSonNode));
        groupResponse.setChildNode(Collections.singletonList(childNode));

        when(innerGroupClient.getGroupListTree(any())).thenReturn(groupResponse);

        bizDemandComponent.getGroupListTreeMap(Arrays.asList(1L, 2L));
    }

    @Test
    public void testGetDeptChainName() {
        GroupResponse groupResponse = new GroupResponse();
        groupResponse.setGroupName("www");
        GroupResponse group = new GroupResponse();
        group.setGroupName("www");
        List<GroupResponse> list = new ArrayList<>();
        list.add(groupResponse);
        list.add(group);
        when(innerGroupClient.getGroupChain(any())).thenReturn(list);

        bizDemandComponent.getDeptChainName(1L);
    }

    @Test
    public void testGetProjectEndDate() {
        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setProductDemandId(1L);
        when(productBizDemandMapper.getByBizDemandId(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        ProjectDO projectDO = new ProjectDO();
        projectDO.setActualEndDate(new Date());
        projectDO.setPlanEndDate(new Date());
        when(projectMapper.selectByProductDemandIdList(any())).thenReturn(Collections.singletonList(projectDO));

        bizDemandComponent.getProjectEndDate(1L);
    }

    @Test
    public void testPage() {
        GroupResponse groupResponse = new GroupResponse();
        groupResponse.setGroupName("www");
        GroupResponse child = new GroupResponse();
        child.setGroupName("www");
        child.setGroupId("1");
        groupResponse.setChildNode(Collections.singletonList(child));
        when(innerGroupClient.getGroupListTree(any())).thenReturn(groupResponse);

        BizDemandListDO bizDemandListDO = new BizDemandListDO();
        bizDemandListDO.setDeptId(1L);
        bizDemandListDO.setStatus(1);
        bizDemandListDO.setPriority(1);
        bizDemandListDO.setPlanReleaseDate(1);
        when(bizDemandMapper.selectList(any())).thenReturn(Collections.singletonList(bizDemandListDO));

        BizDemandListCondition bizDemandListCondition = BizDemandListCondition.builder().deptIdList(Collections.singletonList(1L))
                .createDateStart(new Date()).createDateEnd(new Date()).build();
        assert bizDemandComponent.page(bizDemandListCondition).ifSuccess();
    }

}





































