package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.dto.*;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOnlineListDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.request.HomePageBaseReq;
import com.timevale.forward.facade.api.request.HomePageProjectBoardReq;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.model.base.PageResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * @author by YangXu
 * @date 2022/06/06 11:16
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class HomePageServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private HomePageServiceImpl homePageService;

    @Mock
    private HomePageDataIndicatorComponent homePageDataIndicatorComponent;

    @Mock
    private HomePageProjectOnlineLatelyComponent homePageProjectOnlineLatelyComponent;

    @Mock
    private HomePageRiskWarningComponent homePageRiskWarningComponent;

    @Mock
    private HomePageRiskWarningTaskComponent homePageRiskWarningTaskComponent;

    @Mock
    private HomePageRiskWarningSubmitTestComponent homePageRiskWarningSubmitTestComponent;

    @Mock
    private HomePageProjectBoardComponent homePageProjectBoardComponent;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private BizDemandMapper bizDemandMapper;

    @Mock
    private BugOfflineMapper bugOfflineMapper;

    @Mock
    private BugOnlineMapper bugOnlineMapper;

    @Test
    public void testGetDataIndicator(){
        HomePageDataIndicatorDTO homePageDataIndicatorDTO = new HomePageDataIndicatorDTO();
        when(homePageDataIndicatorComponent.getDataIndicator(any())).thenReturn(homePageDataIndicatorDTO);

        when(innerUserPersonClient.getAllMyStaffWithSelf(any(),any())).thenReturn(Collections.singletonList("user"));

        ProjectDO projectDO = new ProjectDO();
        when(projectMapper.getByTeamMember(any())).thenReturn(Collections.singletonList(projectDO));

        BizDemandListDO bizDemandListDO = new BizDemandListDO();
        when(bizDemandMapper.selectList(any())).thenReturn(Collections.singletonList(bizDemandListDO));

        HomePageBaseReq homePageBaseReq = new HomePageBaseReq();
        homePageBaseReq.setTabType(HomePageTabEnum.TEAM.getCode());
        homePageBaseReq.setUserType(UserTypeEnum.RD.getCode());
        assert homePageService.getDataIndicator(homePageBaseReq).ifSuccess();
    }

    @Test
    public void testGetTodoCard(){
        HomePageBaseReq PDReq = new HomePageBaseReq();
        PDReq.setUserType(UserTypeEnum.PD.getCode());
        PDReq.setTabType(HomePageTabEnum.INDIVIDUAL.getCode());

        HomePageBaseReq RDReq = new HomePageBaseReq();
        RDReq.setUserType(UserTypeEnum.RD.getCode());
        RDReq.setTabType(HomePageTabEnum.INDIVIDUAL.getCode());

        HomePageBaseReq QAReq = new HomePageBaseReq();
        QAReq.setUserType(UserTypeEnum.QA.getCode());
        RDReq.setTabType(HomePageTabEnum.INDIVIDUAL.getCode());

        ProjectDO projectDO = new ProjectDO();
        projectDO.setStatus(ProjectStatusEnum.PLANING.getCode());
        when(projectMapper.getByTeamMember(any())).thenReturn(Collections.singletonList(projectDO));

        BizDemandListDO evaluateDO = new BizDemandListDO();
        evaluateDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        BizDemandListDO receivedDO = new BizDemandListDO();
        receivedDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        when(bizDemandMapper.selectList(any())).thenReturn(Lists.newArrayList(evaluateDO,receivedDO));

        BugOfflineDO openDO = new BugOfflineDO();
        openDO.setOperatorId("SYSTEM");
        openDO.setProposerId("SYSTEM");
        openDO.setStatus(BugStatusEnum.OPEN.getCode());
        BugOfflineDO repairDO = new BugOfflineDO();
        repairDO.setOperatorId("SYSTEM");
        repairDO.setProposerId("SYSTEM");
        repairDO.setStatus(BugStatusEnum.REPAIR.getCode());
        BugOfflineDO acceptanceDO = new BugOfflineDO();
        acceptanceDO.setOperatorId("SYSTEM");
        acceptanceDO.setProposerId("SYSTEM");
        acceptanceDO.setStatus(BugStatusEnum.ACCEPTANCE.getCode());
        BugOfflineDO confirmDO = new BugOfflineDO();
        confirmDO.setOperatorId("SYSTEM");
        confirmDO.setProposerId("SYSTEM");
        confirmDO.setStatus(BugStatusEnum.CONFIRM.getCode());
        when(bugOfflineMapper.selectByMembers(anyList())).thenReturn(Lists.newArrayList(openDO,repairDO,acceptanceDO,confirmDO));

        BugOnlineListDO completeDO = new BugOnlineListDO();
        completeDO.setStatus(BugOnlineStatusEnum.COMPLETE.getCode());
        BugOnlineListDO closeDO = new BugOnlineListDO();
        completeDO.setStatus(BugOnlineStatusEnum.CLOSE.getCode());
        BugOnlineListDO requiredDO = new BugOnlineListDO();
        requiredDO.setStatus(BugOnlineStatusEnum.REQUIRED.getCode());
        when(bugOnlineMapper.selectListByCondition(any())).thenReturn(Lists.newArrayList(completeDO,closeDO,requiredDO));

        assert homePageService.getTodoCard(PDReq).ifSuccess();
        assert homePageService.getTodoCard(RDReq).ifSuccess();
        assert homePageService.getTodoCard(QAReq).ifSuccess();
    }

    @Test
    public void testGetProjectOnlineLately(){
        HomePageProjectOnlineLatelyQueryList queryList = new HomePageProjectOnlineLatelyQueryList();
        queryList.setPageNum(1);
        queryList.setPageSize(10);

        PageResult<HomePageProjectOnlineLatelyDTO> pageResult = new PageResult<>();
        HomePageProjectOnlineLatelyDTO homePageProjectOnlineLatelyDTO = new HomePageProjectOnlineLatelyDTO();
        pageResult.setTotal(1);
        pageResult.setResult(Collections.singletonList(homePageProjectOnlineLatelyDTO));
        when(homePageProjectOnlineLatelyComponent.getProjectOnlineLately(any())).thenReturn(pageResult);

        assert homePageService.getProjectOnlineLately(queryList).ifSuccess();
    }

    @Test
    public void testGetRiskWarning(){
        HomePageBaseReq single = new HomePageBaseReq();
        single.setTabType(HomePageTabEnum.INDIVIDUAL.getCode());
        HomePageBaseReq leader = new HomePageBaseReq();
        leader.setTabType(HomePageTabEnum.TEAM.getCode());


        HomePageRiskWarningDTO nodeOverdue = new HomePageRiskWarningDTO();
        nodeOverdue.setProjectId(1L);
        nodeOverdue.setRiskType(ProjectRiskTypeEnum.NODE_OVERDUE.getCode());
        nodeOverdue.setOverdueDay("1");
        nodeOverdue.setNodeActualDate(new Date());
        nodeOverdue.setPlanEndDate(new Date());

        HomePageRiskWarningDTO nodeEntryOverdue = new HomePageRiskWarningDTO();
        nodeEntryOverdue.setProjectId(1L);
        nodeEntryOverdue.setRiskType(ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode());
        nodeEntryOverdue.setOverdueDay("1");
        nodeEntryOverdue.setNodeActualDate(new Date());
        nodeEntryOverdue.setPlanEndDate(new Date());

        HomePageRiskWarningTaskDTO taskOverdue = new HomePageRiskWarningTaskDTO();
        taskOverdue.setProjectId(1L);
        taskOverdue.setRiskType(ProjectRiskTypeEnum.TASK_OVERDUE.getCode());
        taskOverdue.setOverdueTime("1");
        taskOverdue.setPlanEndDate(new Date());

        HomePageRiskWarningSubmitTestDTO submitFailure = new HomePageRiskWarningSubmitTestDTO();
        submitFailure.setProjectId(1L);
        submitFailure.setRiskType(ProjectRiskTypeEnum.SUBMIT_FAILURE.getCode());
        submitFailure.setPlanEndDate(new Date());

        when(homePageRiskWarningComponent.getRiskWarning(any())).thenReturn(Lists.newArrayList(nodeOverdue, nodeEntryOverdue));
        when(homePageRiskWarningTaskComponent.getRiskWarningTask(any())).thenReturn(Collections.singletonList(taskOverdue));
        when(homePageRiskWarningSubmitTestComponent.getRiskWarningSubmitTest(any())).thenReturn(Collections.singletonList(submitFailure));

        assert homePageService.getRiskWarning(single).ifSuccess();
        assert homePageService.getRiskWarning(leader).ifSuccess();

    }

    @Test
    public void testGetProjectBoard(){
        HomePageProjectBoardReq single = new HomePageProjectBoardReq();
        single.setTabType(HomePageTabEnum.INDIVIDUAL.getCode());
        single.setStartDate(new Date());
        single.setEndDate(new Date());
        single.setDeptIds(new ArrayList<>());
        single.setTeamMembers(new HashSet<>());

        HomePageProjectBoardReq leader = new HomePageProjectBoardReq();
        leader.setTabType(HomePageTabEnum.TEAM.getCode());
        leader.setStartDate(new Date());
        leader.setEndDate(new Date());
        leader.setDeptIds(new ArrayList<>());
        leader.setTeamMembers(new HashSet<>());

        BaseInfoResponse PDInfo = new BaseInfoResponse();
        BaseInfoResponse RDInfo = new BaseInfoResponse();
        BaseInfoResponse QAInfo = new BaseInfoResponse();

        PDInfo.setUserId("zhonggong");
        RDInfo.setUserId("yangxu");
        QAInfo.setUserId("nianci");

        PDInfo.setJobFunction("产品");
        RDInfo.setJobFunction("后端开发");
        QAInfo.setJobFunction("测试");

        PDInfo.setName("产品");
        RDInfo.setName("后端开发");
        QAInfo.setName("测试");

        PDInfo.setAccount("zhonggong");
        RDInfo.setAccount("yangxu");
        QAInfo.setAccount("nianci");

        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Lists.newArrayList(PDInfo,RDInfo,QAInfo));
        when(innerUserPersonClient.getAllMyStaffWithSelfInfo(any(),any())).thenReturn(Lists.newArrayList(PDInfo,RDInfo,QAInfo));

        HomePageProjectBoardDTO PDProjectBoardDTO = new HomePageProjectBoardDTO();
        HomePageProjectBoardDTO RDProjectBoardDTO = new HomePageProjectBoardDTO();
        HomePageProjectBoardDTO QAProjectBoardDTO = new HomePageProjectBoardDTO();
        PDProjectBoardDTO.setUserId("zhonggong");
        RDProjectBoardDTO.setUserId("yangxu");
        QAProjectBoardDTO.setUserId("nianci");
        when(homePageProjectBoardComponent.getProjectBoard(anyList())).thenReturn(Lists.newArrayList(PDProjectBoardDTO,RDProjectBoardDTO,QAProjectBoardDTO));

        assert homePageService.getProjectBoard(single).ifSuccess();
        assert homePageService.getProjectBoard(leader).ifSuccess();
    }
}
