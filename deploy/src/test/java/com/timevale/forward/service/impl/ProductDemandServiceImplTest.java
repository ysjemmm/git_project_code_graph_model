package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.*;
import com.timevale.forward.facade.api.request.ProductBizDemandLinkReq;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/18 14:38
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProductDemandServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProductDemandServiceImpl productDemandService;

    @Mock
    private FileComponent fileComponent;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private ProductDemandMapper productDemandMapper;

    @Mock
    private ProductDemandComponent productDemandComponent;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectComponent projectCmponent;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Mock
    private InnerGroupClient innerGroupClient;

    @Mock
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Mock
    private ProductBizDemandComponent productBizDemandComponent;

    @Mock
    private BizDemandComponent bizDemandComponent;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Mock
    private TaskProductDemandComponent taskProductDemandComponent;

    @Test
    public void testList() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProductDemandListDO productDemandListDO = new ProductDemandListDO();
        productDemandListDO.setStatus(1);
        productDemandListDO.setPriority(1);
        when(productDemandComponent.list(any())).thenReturn(Collections.singletonList(productDemandListDO));

        ProductDemandQueryList productDemandQueryList = new ProductDemandQueryList();
        productDemandQueryList.setAscription("CURRENT_USER");
        productDemandQueryList.setPageNum(1);
        productDemandQueryList.setPageSize(5);
        productDemandQueryList.setOwnerIds(Collections.singletonList("www"));

        assert productDemandService.list(productDemandQueryList).ifSuccess();
        localSessionUtilsMockedStatic.close();
    }


    @Test
    public void testUpdateStatus() {
        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setStatus(0);
        when(productDemandMapper.get(any())).thenReturn(productDemandDO);

        assert productDemandService.updateStatus(1L, -10).ifSuccess();
    }

    @Test
    public void testEnable() {
        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setStatus(-10);
        when(productDemandMapper.get(any())).thenReturn(productDemandDO);

        assert productDemandService.enable(1L).ifSuccess();
    }

    @Test
    public void testAdd() {
        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setStatus(1);
        when(productDemandMapper.getByName(any())).thenReturn(null);

        ProjectDO projectDO = new ProjectDO();
        projectDO.setId(1L);
        projectDO.setStatus(1);
        when(projectMapper.get(any())).thenReturn(projectDO);

        ProductDemandAddReq productDemandAddReq = new ProductDemandAddReq();
        productDemandAddReq.setTypes(Collections.singletonList(1));
        productDemandAddReq.setProjectId(1L);

        assert productDemandService.add(productDemandAddReq).ifSuccess();
    }

    @Test
    public void testModify() {

        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setId(1L);
        when(productDemandMapper.getByName(any())).thenReturn(productDemandDO);

        ProductDemandModifyReq productDemandModifyReq = new ProductDemandModifyReq();
        productDemandModifyReq.setId(1L);
        productDemandModifyReq.setTypes(Collections.singletonList(1));

        assert productDemandService.modify(productDemandModifyReq).ifSuccess();
    }

    @Test
    public void testGet() {

        ProductDemandDetailVO productDemandDetailVO = new ProductDemandDetailVO();
        productDemandDetailVO.setName("www");
        when(productDemandComponent.get(any())).thenReturn(productDemandDetailVO);

        assert productDemandService.get(1L).ifSuccess();
    }

    @Test
    public void testMatchProjectList() {

        when(projectProductDemandMapper.getByProductDemandId(any())).thenReturn(null);

//        when(projectCmponent.page(any(), any())).thenReturn(new QueryResultVO<>());

        ProductDemandLinkProjectQueryList productDemandLinkProjectQueryList = new ProductDemandLinkProjectQueryList();
        productDemandLinkProjectQueryList.setProductDemandId(1L);
        productDemandLinkProjectQueryList.setPageNum(1);
        productDemandLinkProjectQueryList.setPageSize(5);
        productDemandLinkProjectQueryList.setStatus(Collections.singletonList(1));

        assert productDemandService.matchProjectList(productDemandLinkProjectQueryList).ifSuccess();
    }

    @Test
    public void testMatchBizDemandList() {
        when(innerUserPersonClient.getAllMyStaffWithSelf(any(), any())).thenReturn(Collections.singletonList("www"));

        ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
        productBizDemandDO.setBizDemandId(1L);
        when(productBizDemandMapper.select(any())).thenReturn(Collections.singletonList(productBizDemandDO));

        when(bizDemandComponent.page(any())).thenReturn(new QueryResultVO<>());

        ProductDemandLinkBizDemandQueryList productDemandLinkBizDemandQueryList = new ProductDemandLinkBizDemandQueryList();
        productDemandLinkBizDemandQueryList.setStatusList(Collections.singletonList(1));
        productDemandLinkBizDemandQueryList.setPageNum(1);
        productDemandLinkBizDemandQueryList.setPageSize(5);
        PersonQuery personQuery = new PersonQuery();
        personQuery.setUserId("www");
        productDemandLinkBizDemandQueryList.setProductDemandId(1L);
        productDemandLinkBizDemandQueryList.setBizDomainIdList(Collections.singletonList(1L));

        assert productDemandService.matchBizDemandList(productDemandLinkBizDemandQueryList).ifSuccess();
    }

    @Test
    public void testLinkOrUnLinkBizDemand() {

        ProductBizDemandLinkReq productBizDemandLinkReq = new ProductBizDemandLinkReq();
        productBizDemandLinkReq.setBizDemandIds(Collections.singletonList(1L));
        productBizDemandLinkReq.setType(0);

        assert productDemandService.linkOrUnLinkBizDemand(productBizDemandLinkReq).ifSuccess();
    }

    @Test
    public void testLinkProjectList() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setStatus(1);
        projectDO.setPriority(1);
        projectDO.setId(1L);
        when(projectMapper.getByProductDemandId(any())).thenReturn(projectDO);

        assert productDemandService.linkProjectList(1L).getId().equals(1L);
    }

    @Test
    public void testLinkBizDemandList() {

        BizDemandListDO bizDemandListDO = new BizDemandListDO();
        bizDemandListDO.setPriority(1);
        bizDemandListDO.setDeptId(1L);
        when(bizDemandMapper.linkBizDemandList(any())).thenReturn(Collections.singletonList(bizDemandListDO));

        GroupResponse groupResponse = new GroupResponse();
        groupResponse.setGroupName("www");
        groupResponse.setDeleteFlag(1);
        Map<Long, GroupResponse> map = new HashMap<>();
        map.put(1L, groupResponse);
        when(bizDemandComponent.getGroupListTreeMap(any())).thenReturn(map);

        ProductBizDemandQueryList productBizDemandQueryList = new ProductBizDemandQueryList();
        productBizDemandQueryList.setPageNum(1);
        productBizDemandQueryList.setPageSize(5);

        assert productDemandService.linkBizDemandList(productBizDemandQueryList).ifSuccess();
    }

}





















