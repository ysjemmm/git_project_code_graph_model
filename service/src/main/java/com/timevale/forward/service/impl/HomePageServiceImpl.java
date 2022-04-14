package com.timevale.forward.service.impl;

import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.dto.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.request.HomePageBaseReq;
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
import com.timevale.forward.service.utils.aop.LogPoint;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/01/11 17:43
 */
@Slf4j
@LogPoint
@RestService
public class HomePageServiceImpl implements HomePageService {

    @Resource
    private HomePageDataIndicatorComponent homePageDataIndicatorComponent;

    @Resource
    private HomePageProjectOnlineLatelyComponent homePageProjectOnlineLatelyComponent;

    @Resource
    private HomePageProjectBoardComponent homePageProjectBoardComponent;

    @Resource
    private HomePageRiskWarningComponent homePageRiskWarningComponent;

    @Resource
    private HomePageRiskWarningSubmitTestComponent homePageRiskWarningSubmitTestComponent;

    @Resource
    private HomePageRiskWarningTaskComponent homePageRiskWarningTaskComponent;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Resource
    private BugOnlineMapper bugOnlineMapper;

    @Override
    public BaseResult<HomePageDataIndicatorVO> getDataIndicator(HomePageBaseReq homePageBaseReq) {
        HomePageDataIndicatorDTO dataIndicator = homePageDataIndicatorComponent.getDataIndicator(homePageBaseReq);
        return BaseResult.success(HomePageDataIndicatorCopier.INSTANCE.convert(dataIndicator));
    }

    @Override
    public BaseResult<HomePageTodoCardVO> getTodoCard(HomePageBaseReq homePageBaseReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        int taskCount = 0;
        int projectCount = 0;
        int bizDemandCount = 0;
        int bugOnLineCount = 0;
        int bugOfflineCount = 0;

        // 获取我及所有下属
        List<String> allMyStaffWithSelf = Lists.newArrayList(userInfo.getId());

        // 进行中的项目
        List<ProjectDO> projectDOList = projectMapper.selectByTeamMember(allMyStaffWithSelf);
        projectCount = (int) projectDOList.stream().filter(e -> ProjectStatusEnum.ongoing(e.getStatus())).count();

        // 产品添加待处理业务需求，开发测试添加待处理任务
        if (UserTypeEnum.PD.getCode().equals(homePageBaseReq.getUserType())) {
            List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(BizDemandListCondition.builder()
                    .receiveManIdList(allMyStaffWithSelf)
                    .build());
            bizDemandCount = (int) bizDemandListDOList.stream()
                    .filter(e -> e.getStatus().equals(BizDemandStatusEnum.EVALUATE.getCode())).count();
        } else {
            List<TaskDO> taskDOList = taskMapper.selectByExecutorList(Lists.newArrayList(allMyStaffWithSelf));
            taskCount = (int) taskDOList.stream().filter(e -> TaskStatusEnum.ongoing(e.getStatus())).count();

            /*
            * 添加待验证bug
            * 用户身份为研发：我的-待解决线下bug = （ bug打开 +待修复）且（经办人=我）
            * 用户身份为测试：我的-待验证线下bug = （待验收 + 待确认）且（提出人=我）
            * */
            List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByMembers(allMyStaffWithSelf);
            if(UserTypeEnum.RD.getCode().equals(homePageBaseReq.getUserType())){
                bugOfflineCount = (int)bugOfflineDOList.stream()
                        .filter(e -> e.getOperatorId().equals(userInfo.getId())
                                && (BugStatusEnum.OPEN.getCode().equals(e.getStatus()) || BugStatusEnum.REPAIR.getCode().equals(e.getStatus())))
                        .count();
            }else{
                bugOfflineCount = (int)bugOfflineDOList.stream()
                        .filter(e -> e.getProposerId().equals(userInfo.getId())
                                && (BugStatusEnum.ACCEPTANCE.getCode().equals(e.getStatus()) || BugStatusEnum.CONFIRM.getCode().equals(e.getStatus())))
                        .count();
            }
        }

        // 待处理线上bug
        List<BugOnlineListDO> bugOnlineListDOList = bugOnlineMapper.selectListByCondition(BugOnlineListCondition.builder()
                .operatorIdList(Lists.newArrayList(userInfo.getId()))
                .build());
        bugOnLineCount = (int)bugOnlineListDOList.stream()
                .filter(e -> {
                    boolean filter = Objects.equals(BugOnlineStatusEnum.COMPLETE.getCode(), e.getStatus())
                            || Objects.equals(BugOnlineStatusEnum.CLOSE.getCode(), e.getStatus())
                            || Objects.equals(BugOnlineStatusEnum.REQUIRED.getCode(), e.getStatus());
                    return !filter;
                })
                .count();

        HomePageTodoCardVO todoCardVO = new HomePageTodoCardVO();
        todoCardVO.setTaskCount(taskCount);
        todoCardVO.setProjectCount(projectCount);
        todoCardVO.setBizDemandCount(bizDemandCount);
        todoCardVO.setBugOnlineCount(bugOnLineCount);
        todoCardVO.setBugOfflineCount(bugOfflineCount);

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
    public BaseResult<List<HomePageRiskWarningVO>> getRiskWarning(HomePageBaseReq homePageBaseReq) {
        // 查询数据，同时转换为Set去重
        Set<HomePageRiskWarningDTO> riskWarningDTOSet =
                Sets.newHashSet(homePageRiskWarningComponent.getRiskWarning(homePageBaseReq));
        Set<HomePageRiskWarningTaskDTO> riskWarningTaskDTOSet =
                Sets.newHashSet(homePageRiskWarningTaskComponent.getRiskWarningTask(homePageBaseReq));
        Set<HomePageRiskWarningSubmitTestDTO> riskWarningSubmitTestDTOSet =
                Sets.newHashSet(homePageRiskWarningSubmitTestComponent.getRiskWarningSubmitTest(homePageBaseReq));

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

        // 个人 或者 我和我的所有下属信息
        List<BaseInfoResponse> allMyStaffInfoWithSelfInfo;
        if(HomePageTabEnum.INDIVIDUAL.getCode().equals(homePageProjectBoardReq.getTabType())){
            allMyStaffInfoWithSelfInfo = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));
        }else{
            allMyStaffInfoWithSelfInfo = innerUserPersonClient.getAllMyStaffWithSelfInfo(userInfo.getId(), true);
        }
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


        if(HomePageTabEnum.TEAM.getCode().equals(homePageProjectBoardReq.getTabType())){
            // 团队面板,团队成员无项目信息时,也需要展示人员信息
            Map<String, BaseInfoResponse> baseInfoResponseMap = allMyStaffInfoWithSelfInfo
                    .stream().collect(Collectors.toMap(BaseInfoResponse::getAccount, Function.identity()));
            List<String> containProjectInfo = result.stream().map(HomePageProjectBoardVO::getUserId).collect(Collectors.toList());
            allMyStaffNameWithSelf.removeAll(containProjectInfo);

            for (String userId : allMyStaffNameWithSelf) {
                BaseInfoResponse baseInfo = baseInfoResponseMap.get(userId);

                UserTypeEnum userType = JobFunctionEnum.getType(baseInfo.getJobFunction());

                HomePageProjectBoardVO homePageProjectBoardVO = new HomePageProjectBoardVO();
                homePageProjectBoardVO.setUserId(baseInfo.getAccount());
                homePageProjectBoardVO.setUserName(baseInfo.getName());
                homePageProjectBoardVO.setUserType(userType.toString());
                homePageProjectBoardVO.setHomePageProjectDateVOList(Lists.emptyList());
                result.add(homePageProjectBoardVO);
            }
        }

        return BaseResult.success(result);
    }

    public List<HomePageProjectBoardDTO> filterByDate(UserTypeEnum userType, Date startDate, Date endDate, List<HomePageProjectBoardDTO> list) {
        if (userType.equals(UserTypeEnum.PD)) {
            return list.stream().filter(e -> {
                Date nodeStart = DateUtil.min(e.getPlanEndDate(), e.getDemandInternalAudit(), e.getDemandConstrue());
                Date nodeEnd = DateUtil.max(e.getPlanEndDate(), e.getDemandInternalAudit(), e.getDemandConstrue());
                return DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);
            }).collect(Collectors.toList());
        } else if (userType.equals(UserTypeEnum.RD)) {
            return list.stream().filter(e -> {
                Date nodeStart = DateUtil.min(e.getTechnicalDetailReview(), e.getDevelopStart(), e.getSubmitTest());
                Date nodeEnd = DateUtil.max(e.getTechnicalDetailReview(), e.getDevelopStart(), e.getSubmitTest());
                return DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);
            }).collect(Collectors.toList());
        } else if(userType.equals(UserTypeEnum.QA)){
            return list.stream().filter(e -> {
                Date nodeStart = DateUtil.min(e.getWriteTestCases(), e.getUseCaseReview());
                Date nodeEnd = DateUtil.max(e.getWriteTestCases(), e.getUseCaseReview());
                boolean filter = DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);

                nodeStart = DateUtil.min(e.getTestStart(), e.getPublishSimulate(), e.getPublishOfficial());
                nodeEnd = DateUtil.max(e.getTestStart(), e.getPublishSimulate(), e.getPublishOfficial());
                filter = filter ||  DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);

                return filter;
            }).collect(Collectors.toList());
        } else{
            return Lists.emptyList();
        }
    }

}
