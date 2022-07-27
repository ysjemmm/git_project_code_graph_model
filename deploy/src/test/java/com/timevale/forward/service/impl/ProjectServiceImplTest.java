package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.ProjectLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author wangxuan
 * @date 2022-02-18 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectNodeComponent projectNodeComponent;

    @Mock
    private ProjectProductLineComponent projectProductLineComponent;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Mock
    private ProductDemandComponent productDemandComponent;

    @Mock
    private ProductDemandMapper productDemandMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private ProjectComponent projectComponent;

    @Mock
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private TaskComponent taskComponent;

    @Mock
    private TaskProductDemandComponent taskProductDemandComponent;

    @Test
    public void testList() {
        UserInfo userInfo = new UserInfo();
        userInfo.setId("www");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);


        when(personMapper.getMainIds(any(), any(), any())).thenReturn(Arrays.asList(1L, 2L));

        when(projectComponent.page(any(), any())).thenReturn(new QueryResultVO<>());

        ProjectQueryList projectQueryList = new ProjectQueryList();
        projectQueryList.setPageNum(1);
        projectQueryList.setPageSize(5);
        projectQueryList.setAscription("CURRENT_USER");

        assert projectService.list(projectQueryList).ifSuccess();
        localSessionUtilsMockedStatic.close();
    }

    @Test
    public void testUpdateStatus() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setStatus(0);
        when(projectMapper.get(any())).thenReturn(projectDO);

        when(projectMapper.update(any())).thenReturn(1);

        ProjectProductDemandDO projectProductDemandDO = new ProjectProductDemandDO();
        projectProductDemandDO.setProductDemandId(1L);
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Collections.singletonList(projectProductDemandDO));

        when(productDemandMapper.updateByIds(any(), any(),any())).thenReturn(1);

        List<Long> list = new ArrayList<>();
        list.add(1L);
        doNothing().when(productDemandComponent).updateDemandStatusAsProductStatusChange(list, false);

        doNothing().when(taskComponent).updateStatusAsProjectStatusChange(any(), any(), any());

        assert projectService.updateStatus(1L, -10).ifSuccess();
    }

    @Test
    public void testEnable() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setStatus(-10);
        when(projectMapper.get(any())).thenReturn(projectDO);

        when(projectNodeComponent.get((Long) any())).thenReturn(null);

        when(projectMapper.update(any())).thenReturn(1);

        doNothing().when(taskComponent).updateStatusAsProjectStatusChange(any(), any(), any());

        assert projectService.enable(1L, true).ifSuccess();
    }

    @Test
    public void testAdd() {
        when(projectMapper.getByName(any())).thenReturn(null);

        when(projectMapper.insert(any())).thenReturn(1);

        doNothing().when(projectProductLineComponent).add(any(), any());

        doNothing().when(personComponent).add(any(), any(), any());

        ProjectAddReq projectAddReq = new ProjectAddReq();
        projectAddReq.setName("www");
        PersonAddReq personAddReq = new PersonAddReq();
        personAddReq.setUserName("www");
        personAddReq.setUserId("www");
        projectAddReq.setPm(personAddReq);
        projectAddReq.setPds(Collections.singletonList(personAddReq));
        projectAddReq.setTeamMembers(Collections.singletonList(personAddReq));

        assert projectService.add(projectAddReq).ifSuccess();
    }

    @Test
    public void testModify() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setId(1L);
        when(projectMapper.getByName(any())).thenReturn(projectDO);

        when(projectMapper.get(any())).thenReturn(projectDO);

        ProjectModifyReq projectModifyReq = new ProjectModifyReq();
        projectModifyReq.setId(1L);
        PersonAddReq personAddReq = new PersonAddReq();
        personAddReq.setUserName("www");
        personAddReq.setUserId("www");
        projectModifyReq.setPm(personAddReq);
        ProjectNodeAddReq projectNodeAddReq = new ProjectNodeAddReq();
        projectNodeAddReq.setName("www");
        projectModifyReq.setProjectNodes(Collections.singletonList(projectNodeAddReq));
        projectModifyReq.setPds(Collections.singletonList(personAddReq));
        projectModifyReq.setTeamMembers(Collections.singletonList(personAddReq));

        assert projectService.modify(projectModifyReq).ifSuccess();
    }

    @Test
    public void testGet() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setPmId("www");
        projectDO.setStatus(1);
        projectDO.setPriority(1);
        projectDO.setType(1);
        when(projectMapper.get(any())).thenReturn(projectDO);

        ProductLineDO productLineDO = new ProductLineDO();
        productLineDO.setName("www");
        when(productLineMapper.get(any())).thenReturn(Collections.singletonList(productLineDO));

        PersonDO personDO = new PersonDO();
        personDO.setUserId("www");
        when(personComponent.select(any(), any())).thenReturn(Collections.singletonList(personDO));

        when(personComponent.select(any(), any())).thenReturn(Collections.singletonList(personDO));

        ProjectNodeDO projectNodeDO = new ProjectNodeDO();
        projectNodeDO.setName("www");
        when(projectNodeComponent.get((Long) any())).thenReturn(Collections.singletonList(projectNodeDO));

        assert projectService.get(1L).ifSuccess();
    }

    @Test
    public void testMatchProductDemandList() {

        ProjectProductDemandDO projectProductDemandDO = new ProjectProductDemandDO();
        projectProductDemandDO.setProductDemandId(1L);
        when(projectProductDemandMapper.getLinkedProductDemand(any())).thenReturn(Collections.singletonList(projectProductDemandDO));

        ProductDemandListDO productDemandListDO = new ProductDemandListDO();
        productDemandListDO.setStatus(1);
        productDemandListDO.setPriority(1);

        ProjectLinkProductDemandQueryList projectLinkProductDemandQueryList = new ProjectLinkProductDemandQueryList();
        projectLinkProductDemandQueryList.setPageNum(1);
        projectLinkProductDemandQueryList.setPageSize(5);

        assert projectService.matchProductDemandList(projectLinkProductDemandQueryList).ifSuccess();
    }

    @Test
    public void testLinkOrUnLinkProductDemand() {
        ProjectDO projectDO = new ProjectDO();
        projectDO.setType(1);
        when(projectMapper.get(any())).thenReturn(projectDO);

        when(projectProductDemandMapper.getLinkedProductDemand(any())).thenReturn(null);

        ProductDemandDO productDemandDO = new ProductDemandDO();
        productDemandDO.setStatus(1);
        when(productDemandMapper.selectById(any())).thenReturn(productDemandDO);

        ProjectProductDemandLinkReq projectProductDemandLinkReq = new ProjectProductDemandLinkReq();
        projectProductDemandLinkReq.setProductDemandIds(Collections.singletonList(1L));
        projectProductDemandLinkReq.setType(0);

        assert projectService.linkOrUnLinkProductDemand(projectProductDemandLinkReq).ifSuccess();
    }

    @Test
    public void testLinkProductDemandList() {

        ProductDemandListDO productDemandListDO = new ProductDemandListDO();
        productDemandListDO.setStatus(1);
        productDemandListDO.setPriority(1);
        when(productDemandMapper.linkProductDemandList(any())).thenReturn(Collections.singletonList(productDemandListDO));

        ProjectProductDemandQueryList projectProductDemandQueryList = new ProjectProductDemandQueryList();
        projectProductDemandQueryList.setPageNum(1);
        projectProductDemandQueryList.setPageSize(5);

        assert projectService.linkProductDemandList(projectProductDemandQueryList).ifSuccess();
    }


}





































