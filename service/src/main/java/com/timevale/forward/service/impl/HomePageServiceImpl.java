package com.timevale.forward.service.impl;

import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.TaskListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dto.*;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.ProjectDO;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        List<ProjectDO> projectDOList = projectMapper.selectByTeamMember(allMyStaffWithSelf);
        projectCount = (int) projectDOList.stream().filter(e -> ProjectStatusEnum.ongoing(e.getStatus())).count();

        // 产品添加待处理业务需求，开发测试添加待处理任务
        if (userType.equals(UserTypeEnum.PD.toString())) {
            List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(BizDemandListCondition.builder()
                    .receiveManIdList(allMyStaffWithSelf)
                    .build());
            bizDemandCount = (int) bizDemandListDOList.stream()
                    .filter(e -> e.getStatus().equals(BizDemandStatusEnum.EVALUATE.getCode())).count();
        } else {
            List<TaskDO> taskDOList = taskMapper.selectByExecutorList(Lists.newArrayList(allMyStaffWithSelf));
            taskCount = (int) taskDOList.stream().filter(e -> TaskStatusEnum.ongoing(e.getStatus())).count();
        }

        HomePageTodoCardVO todoCardVO = new HomePageTodoCardVO();
        todoCardVO.setTaskCount(taskCount);
        todoCardVO.setProjectCount(projectCount);
        todoCardVO.setBizDemandCount(bizDemandCount);

        return BaseResult.success(todoCardVO);
    }

    @Override
    public BaseResult<PageQueryResult<HomePageProjectOnlineLatelyVO>> getProjectOnlineLately(
            HomePageProjectOnlineLatelyQueryList homePageProjectOnlineLatelyQueryList) {

        PageResult<HomePageProjectOnlineLatelyDTO> homePageProjectOnlineLatelyDTOPageResult =
                homePageProjectOnlineLatelyComponent.getProjectOnlineLately(homePageProjectOnlineLatelyQueryList);
        // 分页配置
        List<HomePageProjectOnlineLatelyVO> homePageProjectOnlineLatelyVOList = HomePageProjectOnlineLatelyCopier
                .INSTANCE.convert(homePageProjectOnlineLatelyDTOPageResult.getResult());
        PageQueryResult<HomePageProjectOnlineLatelyVO> result = PageQueryResult.resResult(homePageProjectOnlineLatelyVOList);

        int total = homePageProjectOnlineLatelyDTOPageResult.getTotal();
        int pageSize = homePageProjectOnlineLatelyQueryList.getPageSize();

        result.setTotalItems(total);
        result.setItemsPerPage(pageSize);
        result.setTotalPages((total - 1) / pageSize + 1);
        result.setCurrentPage(homePageProjectOnlineLatelyQueryList.getPageNum());

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<HomePageRiskWarningVO>> getRiskWarning(String userType) {
        // 查询数据，同时转换为Set去重
        Set<HomePageRiskWarningDTO> riskWarningDTOSet = Sets.newHashSet(homePageRiskWarningComponent.getRiskWarning(userType));
        Set<HomePageRiskWarningTaskDTO> riskWarningTaskDTOSet = Sets.newHashSet(homePageRiskWarningTaskComponent.getRiskWarningTask());
        Set<HomePageRiskWarningSubmitTestDTO> riskWarningSubmitTestDTOSet = Sets.newHashSet(homePageRiskWarningSubmitTestComponent.getRiskWarningSubmitTest());

        // 查询结果中所有的项目id
        Set<Long> projectIdSet = Sets.newHashSet();
        projectIdSet.addAll(riskWarningDTOSet.stream().map(HomePageRiskWarningDTO::getProjectId).collect(Collectors.toSet()));
        projectIdSet.addAll(riskWarningTaskDTOSet.stream().map(HomePageRiskWarningTaskDTO::getProjectId).collect(Collectors.toSet()));
        projectIdSet.addAll(riskWarningSubmitTestDTOSet.stream().map(HomePageRiskWarningSubmitTestDTO::getProjectId).collect(Collectors.toSet()));

        // 初始化结果集
        Map<Long, HomePageRiskWarningVO> result = Maps.newHashMap();
        projectIdSet.forEach(key -> result.put(key, new HomePageRiskWarningVO()));
        result.forEach((key, value) -> {
            value.setHomePageTaskVOList(Lists.emptyList());
            value.setHomePageSubmitTestVOList(Lists.emptyList());
            value.setHomePageProjectNodeVOList(Lists.emptyList());
        });

        // 按项目id分类
        Map<Long, List<HomePageRiskWarningDTO>> riskWarningGroup = riskWarningDTOSet.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningDTO::getProjectId));
        Map<Long, List<HomePageRiskWarningTaskDTO>> riskWarningTaskGroup = riskWarningTaskDTOSet.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningTaskDTO::getProjectId));
        Map<Long, List<HomePageRiskWarningSubmitTestDTO>> riskWarningSubmitTestGroup = riskWarningSubmitTestDTOSet.stream()
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

        // 部门id、员工id非空取交集
        if (!CollectionUtils.isEmpty(deptIds)) {
            for (Long deptId : deptIds) {
                allMyStaffWithSelfSet.retainAll(innerUserPersonClient.getByGroupIdNew(String.valueOf(deptId)));
            }
        }
        if (!CollectionUtils.isEmpty(teamMembers)) {
            allMyStaffWithSelfSet.retainAll(teamMembers);
        }

        // 如果查询条件为空直接返回空数据
        if (CollectionUtils.isEmpty(allMyStaffWithSelfSet)) {
            return BaseResult.success(Lists.emptyList());
        }

        // 查询数据
        List<HomePageProjectBoardDTO> homePageProjectBoardDTOList = homePageProjectBoardComponent.getProjectBoard(userType, Lists.newArrayList(allMyStaffWithSelfSet));

        // 数据分组后转换
        List<HomePageProjectBoardVO> result = Lists.newArrayList();
        Map<String, List<HomePageProjectBoardDTO>> homePageProjectBoardDTOGroup = homePageProjectBoardDTOList
                .stream().collect(Collectors.groupingBy(HomePageProjectBoardDTO::getUserId));

        homePageProjectBoardDTOGroup.forEach((key, value) -> {
            HomePageProjectBoardVO homePageProjectBoardVO = new HomePageProjectBoardVO();

            List<HomePageProjectDateVO> homePageProjectDateVOList = HomePageProjectBoardCopier.INSTANCE.convert(value);

            // 时间过滤
            if (startDate != null) {
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

    public List<HomePageProjectDateVO> filterByDate(String userType, Date startDate, Date endDate, List<HomePageProjectDateVO> list) {
        if(CollectionUtils.isEmpty(list)){
            return Lists.emptyList();
        }
        if (userType.equals(UserTypeEnum.PD.toString())) {
            return list.stream().filter(e -> {
                boolean filter = false;
                if (e.getStartPlan() != null) {
                    filter = DateUtil.inInterval(e.getStartPlan(), startDate, endDate);
                }
                if (e.getDemandInternalAudit() != null) {
                    filter = filter ||  DateUtil.inInterval(e.getDemandInternalAudit(), startDate, endDate);
                }
                if (e.getDemandConstrue() != null) {
                    filter = filter ||  DateUtil.inInterval(e.getDemandConstrue(), startDate, endDate);
                }
                return filter;
            }).collect(Collectors.toList());
        } else if (userType.equals(UserTypeEnum.RD.toString())) {
            return list.stream().filter(e -> {
                boolean filter = false;
                if (e.getTechnicalDetailReview() != null) {
                    filter = DateUtil.inInterval(e.getTechnicalDetailReview(), startDate, endDate);
                }
                if (e.getDevelopStart() != null) {
                    filter = filter ||  DateUtil.inInterval(e.getDevelopStart(), startDate, endDate);
                }
                if (e.getSubmitTest() != null) {
                    filter = filter ||  DateUtil.inInterval(e.getSubmitTest(), startDate, endDate);
                }
                return filter;
            }).collect(Collectors.toList());
        } else {
            return list.stream().filter(e -> {
                boolean filter = false;
                if (e.getUseCaseReview() != null) {
                    filter = DateUtil.inInterval(e.getUseCaseReview(), startDate, endDate);
                }
                if (e.getTestStart() != null) {
                    filter = filter ||  DateUtil.inInterval(e.getTestStart(), startDate, endDate);
                }
                if (e.getPublishSimulate() != null) {
                    filter = filter ||  DateUtil.inInterval(e.getPublishSimulate(), startDate, endDate);
                }
                return filter;
            }).collect(Collectors.toList());
        }
    }

}
