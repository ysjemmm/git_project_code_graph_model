package com.timevale.forward.service.impl;

import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
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
import com.timevale.forward.model.enums.*;
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
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;

import javax.annotation.Resource;
import java.math.BigDecimal;
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
        List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);

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
        Map<Long, HomePageRiskWarningVO> resultMap = Maps.newHashMap();
        projectIdSet.forEach(key -> resultMap.put(key, new HomePageRiskWarningVO()));
        resultMap.forEach((key, value) -> {
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

        // 节点排序
        riskWarningGroup.forEach((key, value) -> value.sort((x, y) -> {
            Integer xOverDueDay = Integer.valueOf(x.getOverdueDay());
            Integer yOverDueDay = Integer.valueOf(y.getOverdueDay());
            return yOverDueDay.compareTo(xOverDueDay);
        }));
        riskWarningTaskGroup.forEach((key, value) -> value.sort((x, y) -> {
            BigDecimal xOverTime = new BigDecimal(x.getOverdueTime());
            BigDecimal yOverTime = new BigDecimal(y.getOverdueTime());
            return yOverTime.compareTo(xOverTime);
        }));

        // 填入数据
        riskWarningGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = resultMap.get(key);
            riskWarningVO.setProjectId(key);
            riskWarningVO.setProjectName(value.get(0).getProjectName());
            riskWarningVO.setPlanEndDate(value.get(0).getPlanEndDate());
            riskWarningVO.setHomePageProjectNodeVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });
        riskWarningTaskGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = resultMap.get(key);
            riskWarningVO.setProjectId(key);
            riskWarningVO.setProjectName(value.get(0).getProjectName());
            riskWarningVO.setPlanEndDate(value.get(0).getPlanEndDate());
            riskWarningVO.setHomePageTaskVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });
        riskWarningSubmitTestGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = resultMap.get(key);
            riskWarningVO.setProjectId(key);
            riskWarningVO.setProjectName(value.get(0).getProjectName());
            riskWarningVO.setPlanEndDate(value.get(0).getPlanEndDate());
            riskWarningVO.setHomePageSubmitTestVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });

        // 按项目计划上线时间排序
        List<HomePageRiskWarningVO> resultList = Lists.newArrayList(resultMap.values());
        resultList.sort(Comparator.comparing(HomePageRiskWarningVO::getPlanEndDate));

        return BaseResult.success(resultList);
    }

    @Override
    public BaseResult<List<HomePageProjectBoardVO>> getProjectBoard(HomePageProjectBoardReq homePageProjectBoardReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 取出查询参数
        Date startDate = DateUtil.getStartOfDay(homePageProjectBoardReq.getStartDate());
        Date endDate = DateUtil.getEndOfDay(homePageProjectBoardReq.getEndDate());
        List<Long> deptIds = homePageProjectBoardReq.getDeptIds();
        List<String> teamMembers = homePageProjectBoardReq.getTeamMembers();

        // 我和我的所有下属信息
        List<BaseInfoResponse> allMyStaffInfoWithSelfInfo =
                innerUserPersonClient.getAllMyStaffWithSelfInfo(userInfo.getId(), false);
        //我和我所有下属的职能类型 Map(userid,jobFunction)
        Map<String, String> allMyStaffInfoWithSelfJobFunction = allMyStaffInfoWithSelfInfo
                .stream()
                .collect(Collectors.toMap(BaseInfoResponse::getAccount,
                        e -> e.getJobFunction() == null ? "": e.getJobFunction(),
                        (old, curr) -> curr));
        // 我和我的下属的所有名字
        Set<String> allMyStaffNameWithSelf = allMyStaffInfoWithSelfInfo
                .stream()
                .map(BaseInfoResponse::getAccount)
                .collect(Collectors.toSet());

        // 部门id、员工id非空取交集
        if (!CollectionUtils.isEmpty(deptIds)) {
            Set<String> deptAllMyStaff = Sets.newHashSet();
            for (Long deptId : deptIds) {
                deptAllMyStaff.addAll(innerUserPersonClient.getByGroupIdNew(String.valueOf(deptId)));
            }
            allMyStaffNameWithSelf.retainAll(deptAllMyStaff);
        }
        if (!CollectionUtils.isEmpty(teamMembers)) {
            allMyStaffNameWithSelf.retainAll(teamMembers);
        }

        // 如果查询条件为空直接返回空数据
        if (CollectionUtils.isEmpty(allMyStaffNameWithSelf)) {
            return BaseResult.success(Lists.emptyList());
        }

        // 查询数据
        List<HomePageProjectBoardDTO> homePageProjectBoardDTOList =
                homePageProjectBoardComponent.getProjectBoard(Lists.newArrayList(allMyStaffNameWithSelf));

        // 数据分组后转换
        List<HomePageProjectBoardVO> result = Lists.newArrayList();
        Map<String, List<HomePageProjectBoardDTO>> homePageProjectBoardDTOGroup = homePageProjectBoardDTOList
                .stream().collect(Collectors.groupingBy(HomePageProjectBoardDTO::getUserId));

        homePageProjectBoardDTOGroup.forEach((key, value) -> {
            HomePageProjectBoardVO homePageProjectBoardVO = new HomePageProjectBoardVO();
            UserTypeEnum userType = JobFunctionEnum.getType(allMyStaffInfoWithSelfJobFunction.get(key));

            // 时间过滤
            if (startDate != null || endDate != null) {
                value = filterByDate(userType, startDate, endDate, value);
            }
            // 项目排序按计划上线时间倒序
            value.sort((x, y) -> y.getPlanEndDate().compareTo(x.getPlanEndDate()));

            List<HomePageProjectDateVO> homePageProjectDateVOList = HomePageProjectBoardCopier.INSTANCE.convert(value);

            // 填充数据
            if(!CollectionUtils.isEmpty(value)){
                homePageProjectBoardVO.setUserId(key);
                homePageProjectBoardVO.setUserName(value.get(0).getUserName());
                homePageProjectBoardVO.setUserType(userType.toString());
                homePageProjectBoardVO.setHomePageProjectDateVOList(homePageProjectDateVOList);
                result.add(homePageProjectBoardVO);
            }
        });

        return BaseResult.success(result);
    }

    public List<HomePageProjectBoardDTO> filterByDate(UserTypeEnum userType, Date startDate, Date endDate, List<HomePageProjectBoardDTO> list) {
        if (userType.equals(UserTypeEnum.PD)) {
            return list.stream().filter(e -> {
                boolean  filter =  DateUtil.inInterval(e.getStartPlan(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getDemandInternalAudit(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getDemandConstrue(), startDate, endDate);
                return filter;
            }).collect(Collectors.toList());
        } else if (userType.equals(UserTypeEnum.RD)) {
            return list.stream().filter(e -> {
                boolean  filter =  DateUtil.inInterval(e.getTechnicalDetailReview(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getDevelopStart(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getSubmitTest(), startDate, endDate);
                return filter;
            }).collect(Collectors.toList());
        } else if(userType.equals(UserTypeEnum.QA)){
            return list.stream().filter(e -> {
                boolean  filter =  DateUtil.inInterval(e.getWriteTestCases(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getUseCaseReview(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getTestStart(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getPublishSimulate(), startDate, endDate);
                filter = filter || DateUtil.inInterval(e.getPublishOfficial(), startDate, endDate);
                return filter;
            }).collect(Collectors.toList());
        } else{
            return Lists.emptyList();
        }
    }

}
