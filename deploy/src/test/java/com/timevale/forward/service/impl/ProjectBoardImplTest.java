package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.BugStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.model.enums.TestBillStatusEnum;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectBoardImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectBoardServiceImpl projectBoardService;

    @Mock
    ProjectMapper projectMapper;

    @Mock
    ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    TaskMapper taskMapper;

    @Mock
    TaskProductDemandMapper taskProductDemandMapper;

    @Mock
    TestBillMapper testBillMapper;

    @Mock
    BugOfflineMapper bugOfflineMapper;

    @Mock
    ProjectRiskMapper projectRiskMapper;

    @Mock
    BugLogMapper bugLogMapper;

    @Mock
    PersonMapper personMapper;

    @Test
    public void testGetDataIndicator(){
        List<ProjectProductDemandDO> projectProductDemandDOList = new ArrayList<>();
        projectProductDemandDOList.add(new ProjectProductDemandDO(){{setProductDemandId(1L);}});
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(projectProductDemandDOList);

        List<TaskDO> taskDOList = new ArrayList<>();
        taskDOList.add(new TaskDO(){{
            setStatus(TaskStatusEnum.PROGRESS.getCode());
            setPlanUseTime(new BigDecimal("1"));
            setPlanEndDate(new Date());
        }});
        when(taskMapper.getByProjectId(any())).thenReturn(taskDOList);

        List<BugOfflineDO> bugOfflineDOList = new ArrayList<>();
        bugOfflineDOList.add(new BugOfflineDO(){{
            setStatus(BugStatusEnum.REPAIR.getCode());
        }});
        when(bugOfflineMapper.selectByProjectId(any())).thenReturn(bugOfflineDOList);

        when(testBillMapper.selectByProjectId(any())).thenReturn(new TestBillDO(){{
            setStatus(TestBillStatusEnum.TEST_SUCCESS.getCode());
        }});

        List<TaskProductDemandDO> taskProductDemandDOList = new ArrayList<>();
        taskProductDemandDOList.add(new TaskProductDemandDO());
        when(taskProductDemandMapper.selectByProductDemandId(any())).thenReturn(taskProductDemandDOList);

        when(projectRiskMapper.selectByProjectIdList(anyList())).thenReturn(new ArrayList<>());

        assert projectBoardService.getDataIndicator(1L).ifSuccess();
    }

    @Test
    public void testGetBoardBugOfflineTrend(){
        when(projectMapper.get(any())).thenReturn(new ProjectDO(){{
            setPlanStartDate(new Date());
            setPlanEndDate(new Date());
        }});

        List<BugOfflineDO> bugOfflineDOList = new ArrayList<>();
        bugOfflineDOList.add(new BugOfflineDO(){{
            setId(1L);
            setCreateDate(new Date());
        }});
        when(bugOfflineMapper.selectByProjectId(any())).thenReturn(bugOfflineDOList);

        List<BugLogDO> bugLogDOList = new ArrayList<>();
        bugLogDOList.add(new BugLogDO(){{
            setNewValue(BugStatusEnum.COMPLETE.getText());
            setCreateDate(new Date());
            setMainId(1L);
        }});
        when(bugLogMapper.selectBugStatusLog(anyList(),any())).thenReturn(bugLogDOList);

        assert projectBoardService.getBoardBugOfflineTrend(1L).ifSuccess();
    }


}
