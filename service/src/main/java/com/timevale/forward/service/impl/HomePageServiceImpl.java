package com.timevale.forward.service.impl;

import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dto.*;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.request.HomePageProjectBoardReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.model.enums.UserTypeEnum;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.copy.HomePageDataIndicatorCopier;
import com.timevale.forward.service.copy.HomePageProjectBoardCopier;
import com.timevale.forward.service.copy.HomePageProjectOnlineLatelyCopier;
import com.timevale.forward.service.copy.HomePageRiskWarningCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.model.base.PageResult;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/01/11 17:43
 */
@Slf4j
@RestService
public class HomePageServiceImpl implements HomePageService {

    @Resource
    HomePageDataIndicatorComponent homePageDataIndicatorComponent;

    @Resource
    HomePageProjectOnlineLatelyComponent homePageProjectOnlineLatelyComponent;

    @Resource
    HomePageProjectBoardComponent homePageProjectBoardComponent;

    @Resource
    HomePageRiskWarningComponent homePageRiskWarningComponent;

    @Resource
    HomePageRiskWarningSubmitTestComponent homePageRiskWarningSubmitTestComponent;

    @Resource
    HomePageRiskWarningTaskComponent homePageRiskWarningTaskComponent;

    @Resource
    InnerUserPersonClient innerUserPersonClient;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    TaskMapper taskMapper;

    @Override
    public BaseResult<HomePageDataIndicatorVO> getDataIndicator(String userType) {
        HomePageDataIndicatorDTO dataIndicator = homePageDataIndicatorComponent.getDataIndicator(userType);
        return BaseResult.success(HomePageDataIndicatorCopier.INSTANCE.convert(dataIndicator));
    }

    @Override
    public BaseResult<HomePageTodoCardVO> getTodoCard(String userType) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        int taskCount = 0;
        int projectCount = 0;
        int bizDemandCount = 0;

        // 获取我及所有下属
        List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());

        // 进行中的项目
        List<ProjectListDO> projectListDOList = projectMapper.list(ProjectListCondition.builder()
                .teamMembers(allMyStaffWithSelf)
                .build());
        projectCount = Math.toIntExact(projectListDOList.stream()
                .filter(e -> ProjectStatusEnum.ongoing(e.getStatus())).count());

        // 如果为产品则添加待处理业务需求，否则添加待处理任务
        if(userType.equals(UserTypeEnum.PD.toString())){
            // 待处理业务
            List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(BizDemandListCondition.builder()
                    .receiveManIdList(allMyStaffWithSelf)
                    .build());
            bizDemandCount = Math.toIntExact(bizDemandListDOList.stream()
                    .filter(e -> e.getStatus().equals(BizDemandStatusEnum.EVALUATE.getCode())).count());
        }else{
            List<TaskDO> taskDOList = taskMapper.list(TaskListCondition.builder()
                    .executorIds(allMyStaffWithSelf)
                    .build());
            taskCount = Math.toIntExact(taskDOList.stream()
                    .filter(e -> TaskStatusEnum.ongoing(e.getStatus())).count());
        }

        HomePageTodoCardVO todoCardVO = new HomePageTodoCardVO();
        todoCardVO.setTaskCount(taskCount);
        todoCardVO.setProjectCount(projectCount);
        todoCardVO.setBizDemandCount(bizDemandCount);

        return BaseResult.success(todoCardVO);
    }

    @Override
    public BaseResult<PageQueryResult<HomePageProjectOnlineLatelyVO>> getProjectOnlineLately(HomePageProjectOnlineLatelyQueryList homePageProjectOnlineLatelyQueryList) {
        PageResult<HomePageProjectOnlineLatelyDTO> homePageProjectOnlineLatelyDTOPageResult = homePageProjectOnlineLatelyComponent.getProjectOnlineLately(homePageProjectOnlineLatelyQueryList);
        PageQueryResult<HomePageProjectOnlineLatelyVO> result = PageQueryResult.resResult(HomePageProjectOnlineLatelyCopier.INSTANCE.convert(homePageProjectOnlineLatelyDTOPageResult.getResult()));
        result.setCurrentPage(homePageProjectOnlineLatelyQueryList.getPageNum());
        result.setTotalItems(homePageProjectOnlineLatelyDTOPageResult.getTotal());
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<HomePageRiskWarningVO>> getRiskWarning(String userType) {
        List<HomePageRiskWarningDTO> riskWarningDTOList = homePageRiskWarningComponent.getRiskWarning(userType);
        List<HomePageRiskWarningTaskDTO> riskWarningTaskDTOList = homePageRiskWarningTaskComponent.getRiskWarningTask();
        List<HomePageRiskWarningSubmitTestDTO> riskWarningSubmitTestDTOList = homePageRiskWarningSubmitTestComponent.getRiskWarningSubmitTest();

        // 结果集
        Map<Long, HomePageRiskWarningVO> result = Maps.newHashMap();

        // 查询结果中所有的项目
        Set<Long> projectIdSet = Sets.newHashSet();
        projectIdSet.addAll(riskWarningDTOList.stream().map(HomePageRiskWarningDTO::getProjectId).collect(Collectors.toSet()));
        projectIdSet.addAll(riskWarningTaskDTOList.stream().map(HomePageRiskWarningTaskDTO::getProjectId).collect(Collectors.toSet()));
        projectIdSet.addAll(riskWarningSubmitTestDTOList.stream().map(HomePageRiskWarningSubmitTestDTO::getProjectId).collect(Collectors.toSet()));
        projectIdSet.forEach(key -> result.put(key, new HomePageRiskWarningVO()));

        // 初始化结果集中集合
        result.forEach((key, value) -> {
            value.setHomePageTaskVOList(Lists.emptyList());
            value.setHomePageSubmitTestVOList(Lists.emptyList());
            value.setHomePageProjectNodeVOList(Lists.emptyList());
        });

        // 按项目分类
        Map<Long, List<HomePageRiskWarningDTO>> riskWarningGroup = riskWarningDTOList.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningDTO::getProjectId));
        Map<Long, List<HomePageRiskWarningTaskDTO>> riskWarningTaskGroup = riskWarningTaskDTOList.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningTaskDTO::getProjectId));
        Map<Long, List<HomePageRiskWarningSubmitTestDTO>> riskWarningSubmitTestGroup = riskWarningSubmitTestDTOList.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningSubmitTestDTO::getProjectId));

        // 填入数据
        riskWarningGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = result.get(key);
            riskWarningVO.setProjectId(key);
            riskWarningVO.setProjectName(value.get(0).getProjectName());
            riskWarningVO.setHomePageProjectNodeVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });
        riskWarningTaskGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = result.get(key);
            riskWarningVO.setProjectId(key);
            riskWarningVO.setProjectName(value.get(0).getProjectName());
            riskWarningVO.setHomePageTaskVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });
        riskWarningSubmitTestGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = result.get(key);
            riskWarningVO.setProjectId(key);
            riskWarningVO.setProjectName(value.get(0).getProjectName());
            riskWarningVO.setHomePageSubmitTestVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });

        return BaseResult.success(Lists.newArrayList(result.values()));
    }

    @Override
    public BaseResult<List<HomePageProjectBoardVO>> getProjectBoard(HomePageProjectBoardReq homePageProjectBoardReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 取出查询参数
        String userType = homePageProjectBoardReq.getUserType();
        Date startDate = homePageProjectBoardReq.getStartDate();
        Date endDate = homePageProjectBoardReq.getEndDate();
        List<Long> deptIds = homePageProjectBoardReq.getDeptIds();
        List<String> teamMembers = homePageProjectBoardReq.getTeamMembers();

        // 我和我的所有员工 Set
        Set<String> allMyStaffWithSelfSet = Sets.newHashSet(innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId()));

        // 部门id非空取交集
        if(!CollectionUtils.isEmpty(deptIds)){
            for (Long deptId : deptIds) {
                allMyStaffWithSelfSet.retainAll(innerUserPersonClient.getByGroupIdNew(String.valueOf(deptId)));
            }
        }

        // 员工id非空取交集
        if(!CollectionUtils.isEmpty(teamMembers)){
            allMyStaffWithSelfSet.retainAll(teamMembers);
        }

        // 查询数据
        List<HomePageProjectBoardDTO> homePageProjectBoardDTOList = homePageProjectBoardComponent.getProjectBoard(userType, new ArrayList<>(allMyStaffWithSelfSet));

        // 数据分组后转换
        List<HomePageProjectBoardVO> result = Lists.newArrayList();
        Map<String, List<HomePageProjectBoardDTO>> homePageProjectBoardDTOGroup = homePageProjectBoardDTOList.stream().collect(Collectors.groupingBy(HomePageProjectBoardDTO::getUserId));
        homePageProjectBoardDTOGroup.forEach((key, value) -> {
            HomePageProjectBoardVO homePageProjectBoardVO = new HomePageProjectBoardVO();

            List<HomePageProjectDateVO> homePageProjectDateVOList = HomePageProjectBoardCopier.INSTANCE.convert(value);

            // 时间过滤
            if(startDate != null){
                homePageProjectDateVOList = filterByDate(userType, startDate, endDate, homePageProjectDateVOList);
            }

            // 填充数据
            homePageProjectBoardVO.setUserId(key);
            homePageProjectBoardVO.setUserName(value.get(0).getUserName());
            homePageProjectBoardVO.setHomePageProjectDateVOList(homePageProjectDateVOList);
            result.add(homePageProjectBoardVO);
        });

        return BaseResult.success(result);
    }

    public List<HomePageProjectDateVO> filterByDate(String userType, Date startDate, Date endDate, List<HomePageProjectDateVO> list){
        if(userType.equals(UserTypeEnum.PD.toString())){
            return list.stream().filter(e -> {
                if(e.getStartPlan() != null){
                    return DateUtil.inInterval(e.getStartPlan(), startDate, endDate);
                }
                if(e.getDemandInternalAudit() != null){
                    return DateUtil.inInterval(e.getDemandInternalAudit(), startDate, endDate);
                }
                if(e.getDemandConstrue() != null){
                    return DateUtil.inInterval(e.getDemandConstrue(), startDate, endDate);
                }
                return false;
            }).collect(Collectors.toList());
        }else if(userType.equals(UserTypeEnum.RD.toString())){
            return list.stream().filter(e -> {
                if(e.getTechnicalDetailReview() != null){
                    return DateUtil.inInterval(e.getTechnicalDetailReview(), startDate, endDate);
                }
                if(e.getDevelopStart() != null){
                    return DateUtil.inInterval(e.getDevelopStart(), startDate, endDate);
                }
                if(e.getSubmitTest() != null){
                    return DateUtil.inInterval(e.getSubmitTest(), startDate, endDate);
                }
                return false;
            }).collect(Collectors.toList());
        }else{
            return list.stream().filter(e -> {
                if(e.getUseCaseReview() != null){
                    return DateUtil.inInterval(e.getUseCaseReview(), startDate, endDate);
                }
                if(e.getTestStart() != null){
                    return DateUtil.inInterval(e.getTestStart(), startDate, endDate);
                }
                if(e.getPublishSimulate() != null){
                    return DateUtil.inInterval(e.getPublishSimulate(), startDate, endDate);
                }
                return false;
            }).collect(Collectors.toList());
        }
    }

}
