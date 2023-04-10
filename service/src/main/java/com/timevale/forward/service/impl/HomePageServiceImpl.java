package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.dto.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.request.HomePageBaseReq;
import com.timevale.forward.facade.api.request.HomePageHolidayReq;
import com.timevale.forward.facade.api.request.HomePageProjectBoardReq;
import com.timevale.forward.facade.api.request.HomePageTaskBoardReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
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
import org.apache.commons.collections4.CollectionUtils;
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
    private DistributionComponent distributionComponent;

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

    @Resource
    private PersonMapper personMapper;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Override
    public BaseResult<HomePageDataIndicatorVO> getDataIndicator(HomePageBaseReq homePageBaseReq) {
        HomePageDataIndicatorDTO dataIndicator = homePageDataIndicatorComponent.getDataIndicator(homePageBaseReq);
        HomePageDataIndicatorVO dataIndicatorVO = HomePageDataIndicatorCopier.INSTANCE.convert(dataIndicator);

        // 如果为团队tab
        if (HomePageTabEnum.TEAM.getCode().equals(homePageBaseReq.getTabType())) {
            // 成员信息
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);

            // 项目信息
            List<ProjectDO> projectDOList = projectMapper.getByTeamMember(allMyStaffWithSelf);
            projectDOList = projectDOList.stream().filter(e -> !ProjectStatusEnum.INVALID.getCode().equals(e.getStatus())).collect(Collectors.toList());

            dataIndicatorVO.setProjectReadyStartCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_START.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectReadyInternalAuditCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_INTERNAL_AUDIT.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectReadyConstrueCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_CONSTRUE.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectReadyConstrueReverseCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_CONSTRUE_REVERSE.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectReadyUedAuditCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_UED_AUDIT.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectReadyTechnicalDetailReviewCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_TECHNICAL_DETAIL_REVIEW.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectReadyDevelopCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_DEVELOP.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectDevelopingCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.DEVELOPING.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectReadyTestCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.READY_TEST.getCode().equals(e.getNodeStatus())).count());
            dataIndicatorVO.setProjectTestingCount((int) projectDOList.stream().filter(e -> ProjectNodeStatusEnum.TESTING.getCode().equals(e.getNodeStatus())).count());

            // 需求信息
            List<BizDemandListDO> bizdemandDOList = bizDemandMapper.selectList(BizDemandListCondition
                    .builder()
                    .receiveManIdList(allMyStaffWithSelf)
                    .build());
            dataIndicatorVO.setBizDemandReadyDealWithCount((int) bizdemandDOList.stream().filter(e -> BizDemandStatusEnum.EVALUATE.getCode().equals(e.getStatus())).count());
            dataIndicatorVO.setBizDemandReadyScheduleCount((int) bizdemandDOList.stream().filter(e -> BizDemandStatusEnum.RECEIVED.getCode().equals(e.getStatus())).count());
        }

        return BaseResult.success(dataIndicatorVO);
    }

    @Override
    public BaseResult<HomePageTodoCardVO> getTodoCard(HomePageBaseReq homePageBaseReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        int projectCount;
        int bugOnLineCount;
        int taskCount = 0;
        int bizDemandCount = 0;
        int bugOfflineCount = 0;
        int bizDemandReceivedCount = 0;

        // 获取我及所有下属
        List<String> allMyStaffWithSelf = Lists.newArrayList(userInfo.getId());

        // 进行中的项目
        List<ProjectDO> projectDOList = projectMapper.getByTeamMember(allMyStaffWithSelf);
        projectCount = (int) projectDOList.stream().filter(e -> ProjectStatusEnum.ongoing(e.getStatus())).count();

        // 产品添加待处理业务需求，开发测试添加待处理任务
        if (UserTypeEnum.PD.getCode().equals(homePageBaseReq.getUserType())) {
            List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(BizDemandListCondition.builder()
                    .receiveManIdList(allMyStaffWithSelf)
                    .build());
            bizDemandCount = (int) bizDemandListDOList.stream()
                    .filter(e -> e.getStatus().equals(BizDemandStatusEnum.EVALUATE.getCode())).count();
            bizDemandReceivedCount = (int) bizDemandListDOList.stream()
                    .filter(e -> e.getStatus().equals(BizDemandStatusEnum.RECEIVED.getCode())).count();
        } else {
            List<TaskDO> taskDOList = taskMapper.selectByExecutorList(Lists.newArrayList(allMyStaffWithSelf));
            taskCount = (int) taskDOList.stream().filter(e -> TaskStatusEnum.ongoing(e.getStatus())).count();

            /*
             * 添加待验证bug
             * 用户身份为研发：我的-待解决线下bug = （ bug打开 +待修复）且（经办人=我）
             * 用户身份为测试：我的-待验证线下bug = （待验收 + 待确认）且（提出人=我）
             * */
            List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByMembers(allMyStaffWithSelf);
            if (UserTypeEnum.RD.getCode().equals(homePageBaseReq.getUserType())) {
                bugOfflineCount = (int) bugOfflineDOList.stream()
                        .filter(e -> e.getOperatorId().equals(userInfo.getId())
                                && (BugStatusEnum.OPEN.getCode().equals(e.getStatus()) || BugStatusEnum.REPAIR.getCode().equals(e.getStatus())))
                        .count();
            } else {
                bugOfflineCount = (int) bugOfflineDOList.stream()
                        .filter(e -> e.getProposerId().equals(userInfo.getId())
                                && (BugStatusEnum.ACCEPTANCE.getCode().equals(e.getStatus()) || BugStatusEnum.CONFIRM.getCode().equals(e.getStatus())))
                        .count();
            }
        }

        // 待处理线上bug
        List<BugOnlineListDO> bugOnlineListDOList = bugOnlineMapper.selectListByCondition(BugOnlineListCondition.builder()
                .operatorIdList(Lists.newArrayList(userInfo.getId()))
                .build());
        bugOnLineCount = (int) bugOnlineListDOList.stream()
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
        todoCardVO.setBizDemandReceivedCount(bizDemandReceivedCount);

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
        // 查询数据
        List<HomePageRiskWarningDTO> riskWarningDTOList = homePageRiskWarningComponent.getRiskWarning(homePageBaseReq);
        List<HomePageRiskWarningTaskDTO> warningTaskDTOList = homePageRiskWarningTaskComponent.getRiskWarningTask(homePageBaseReq);
        List<HomePageRiskWarningSubmitTestDTO> submitTestDTOList = homePageRiskWarningSubmitTestComponent.getRiskWarningSubmitTest(homePageBaseReq);

        // 查询结果中所有的项目id
        Set<Long> projectIdSet = Sets.newHashSet();
        projectIdSet.addAll(riskWarningDTOList.stream().map(HomePageRiskWarningDTO::getProjectId).collect(Collectors.toSet()));
        projectIdSet.addAll(warningTaskDTOList.stream().map(HomePageRiskWarningTaskDTO::getProjectId).collect(Collectors.toSet()));
        projectIdSet.addAll(submitTestDTOList.stream().map(HomePageRiskWarningSubmitTestDTO::getProjectId).collect(Collectors.toSet()));

        // 判空处理
        if (CollUtil.isEmpty(projectIdSet)) {
            return BaseResult.success(Lists.emptyList());
        }

        // 查询项目
        List<ProjectDO> projectDOs = projectMapper.getByIds(projectIdSet);
        ImmutableMap<Long, ProjectDO> projectDOMap = Maps.uniqueIndex(projectDOs, BaseDO::getId);

        // 初始化结果集
        Map<Long, HomePageRiskWarningVO> resultMap = Maps.newHashMap();
        projectIdSet.forEach(key -> resultMap.put(key, new HomePageRiskWarningVO()));
        resultMap.forEach((key, value) -> {
            value.setHomePageTaskVOList(Lists.emptyList());
            value.setHomePageSubmitTestVOList(Lists.emptyList());
            value.setHomePageProjectNodeVOList(Lists.emptyList());
        });

        // 按项目id分类
        Map<Long, List<HomePageRiskWarningDTO>> riskWarningGroup = riskWarningDTOList.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningDTO::getProjectId));
        Map<Long, List<HomePageRiskWarningTaskDTO>> riskWarningTaskGroup = warningTaskDTOList.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningTaskDTO::getProjectId));
        Map<Long, List<HomePageRiskWarningSubmitTestDTO>> riskWarningSubmitTestGroup = submitTestDTOList.stream()
                .collect(Collectors.groupingBy(HomePageRiskWarningSubmitTestDTO::getProjectId));

        log.info("[getRiskWarning]项目风险map：{}", riskWarningGroup);

        // TL 身份保留一个
        if (HomePageTabEnum.TEAM.getCode().equals(homePageBaseReq.getTabType())) {
            riskWarningGroup.forEach((k, v) -> {
                Optional<HomePageRiskWarningDTO> max = v.stream()
                        .filter(e -> ProjectRiskTypeEnum.NODE_OVERDUE.getCode().equals(e.getRiskType()))
                        .max((a, b) -> {
                            int compare = a.getNodeActualDate().compareTo(b.getNodeActualDate());
                            if (compare == 0) {
                                Integer aCode = ProjectNodeEnum.getCodeByName(a.getNodeName());
                                Integer bCode = ProjectNodeEnum.getCodeByName(b.getNodeName());
                                return aCode.compareTo(bCode);
                            }
                            return compare;
                        });
                v.removeIf(e -> ProjectRiskTypeEnum.NODE_OVERDUE.getCode().equals(e.getRiskType()));
                max.ifPresent(v::add);
            });
        }

        log.info("[getRiskWarning]项目风险过滤过程预期：{}", riskWarningGroup);

        // 节点排序
        riskWarningGroup.forEach((k, v) -> v.sort((x, y) -> {
            Integer xOverDueDay = Integer.valueOf(x.getOverdueDay());
            Integer yOverDueDay = Integer.valueOf(y.getOverdueDay());
            return yOverDueDay.compareTo(xOverDueDay);
        }));
        riskWarningTaskGroup.forEach((k, v) -> v.sort((x, y) -> {
            BigDecimal xOverTime = new BigDecimal(x.getOverdueTime());
            BigDecimal yOverTime = new BigDecimal(y.getOverdueTime());
            return yOverTime.compareTo(xOverTime);
        }));

        // 填入数据
        riskWarningGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = resultMap.get(key);
            ProjectDO projectDO = projectDOMap.get(key);
            if (projectDO != null) {
                riskWarningVO.setProjectId(key);
                riskWarningVO.setProjectName(projectDO.getName());
                riskWarningVO.setCategory(projectDO.getCategory());
                riskWarningVO.setPlanEndDate(projectDO.getPlanEndDate());
            }
            riskWarningVO.setHomePageProjectNodeVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });
        riskWarningTaskGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = resultMap.get(key);
            ProjectDO projectDO = projectDOMap.get(key);
            if (projectDO != null) {
                riskWarningVO.setProjectId(key);
                riskWarningVO.setProjectName(projectDO.getName());
                riskWarningVO.setCategory(projectDO.getCategory());
                riskWarningVO.setPlanEndDate(projectDO.getPlanEndDate());
            }
            riskWarningVO.setHomePageTaskVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });
        riskWarningSubmitTestGroup.forEach((key, value) -> {
            HomePageRiskWarningVO riskWarningVO = resultMap.get(key);
            ProjectDO projectDO = projectDOMap.get(key);
            if (projectDO != null) {
                riskWarningVO.setProjectId(key);
                riskWarningVO.setProjectName(projectDO.getName());
                riskWarningVO.setCategory(projectDO.getCategory());
                riskWarningVO.setPlanEndDate(projectDO.getPlanEndDate());
            }
            riskWarningVO.setHomePageSubmitTestVOList(value.stream().map(HomePageRiskWarningCopier.INSTANCE::convert).collect(Collectors.toList()));
        });

        log.info("[getRiskWarning]项目风险结果：{}", resultMap);

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
        if (HomePageTabEnum.INDIVIDUAL.getCode().equals(homePageProjectBoardReq.getTabType())) {
            allMyStaffInfoWithSelfInfo = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));
        } else {
            allMyStaffInfoWithSelfInfo = innerUserPersonClient.getAllMyStaffWithSelfInfo(userInfo.getId(), false);
        }
        //我和我所有下属的职能类型 Map(userid,jobFunction)
        Map<String, String> allMyStaffInfoWithSelfJobFunction = allMyStaffInfoWithSelfInfo
                .stream()
                .collect(Collectors.toMap(BaseInfoResponse::getAccount,
                        e -> e.getJobFunction() == null ? "" : e.getJobFunction(),
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
            if (!CollectionUtils.isEmpty(value)) {
                homePageProjectBoardVO.setUserId(key);
                homePageProjectBoardVO.setUserName(value.get(0).getUserName());
                homePageProjectBoardVO.setUserType(userType.toString());
                homePageProjectBoardVO.setHomePageProjectDateVOList(homePageProjectDateVOList);
                result.add(homePageProjectBoardVO);
            }
        });

        if (HomePageTabEnum.TEAM.getCode().equals(homePageProjectBoardReq.getTabType())) {
            // 团队面板,团队成员无项目信息时,也需要展示人员信息
            Map<String, BaseInfoResponse> baseInfoResponseMap = allMyStaffInfoWithSelfInfo
                    .stream().collect(Collectors.toMap(BaseInfoResponse::getAccount, Function.identity()));
            List<String> containProjectInfo = result.stream().map(HomePageProjectBoardVO::getUserId).collect(Collectors.toList());

            containProjectInfo.forEach(allMyStaffNameWithSelf::remove);

            for (String userId : allMyStaffNameWithSelf) {
                BaseInfoResponse baseInfo = baseInfoResponseMap.get(userId);

                UserTypeEnum userType = JobFunctionEnum.getType(baseInfo.getJobFunction());

                HomePageProjectBoardVO homePageProjectBoardVO = new HomePageProjectBoardVO();
                homePageProjectBoardVO.setUserId(baseInfo.getAccount());
                homePageProjectBoardVO.setUserName(baseInfo.getAlias() + CommonConstant.JOIN_LINE + baseInfo.getName());
                homePageProjectBoardVO.setUserType(userType.toString());
                homePageProjectBoardVO.setHomePageProjectDateVOList(Lists.emptyList());
                result.add(homePageProjectBoardVO);
            }
        }

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<UpdateTimeVO> getUpdateTime() {
        UpdateTimeDTO updateTimeDTO = distributionComponent.getUpdateDate();
        UpdateTimeVO updateTimeVO = DistributionCopier.INSTANCE.convert(updateTimeDTO);

        return BaseResult.success(updateTimeVO);
    }

    @Override
    public BaseResult<List<HomePageSingleWorkTimeVO>> getTaskWorkTimeBoard(HomePageTaskBoardReq req) {
        log.info("首页任务看板,参数:{}", req);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        Date startDate = DateUtil.getStartOfDay(req.getStartDate());
        Date endDate = DateUtil.getEndOfDay(req.getEndDate());
        Set<String> deptIds = req.getDeptIds();
        Set<String> teamMembers = req.getTeamMembers();

        // 个人 或者 我和我的所有下属信息
        List<BaseInfoResponse> responses;
        if (HomePageTabEnum.INDIVIDUAL.getCode().equals(req.getTabType())) {
            responses = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));
        } else {
            responses = innerUserPersonClient.getAllMyStaffWithSelfInfo(userInfo.getId(), false);
        }
        // 我和我的下属的所有名字
        Set<String> allMyStaffNameWithSelf = responses.stream().map(BaseInfoResponse::getAccount).collect(Collectors.toSet());
        Map<String, String> personMap = responses.stream()
                .collect(Collectors.toMap(BaseInfoResponse::getAccount, a -> a.getAlias() + "-" + a.getName(), (v1, v2) -> v2));

        // 部门id、员工id非空取交集
        if (CollUtil.isNotEmpty(deptIds)) {
            Set<String> deptAllMyStaff = Sets.newHashSet();
            for (String deptId : deptIds) {
                deptAllMyStaff.addAll(innerUserPersonClient.getByGroupIdNew(deptId));
            }
            allMyStaffNameWithSelf.retainAll(deptAllMyStaff);
        }
        if (CollUtil.isNotEmpty(teamMembers)) {
            allMyStaffNameWithSelf.retainAll(teamMembers);
        }
        // 如果查询条件为空直接返回空数据
        if (CollUtil.isEmpty(allMyStaffNameWithSelf)) {
            return BaseResult.success(Lists.emptyList());
        }

        List<TaskBoardDTO> filter = listTasksSuitDateRange(startDate, endDate, allMyStaffNameWithSelf);

        if (CollUtil.isNotEmpty(filter)) {
            List<HomePageSingleWorkTimeVO> result = new ArrayList<>();

            List<Long> filterIds = filter.stream().map(TaskBoardDTO::getId).collect(Collectors.toList());
            Map<Long, TaskBoardDTO> taskMap = filter.stream().collect(Collectors.toMap(TaskBoardDTO::getId, k -> k, (v1, v2) -> v2));
            Map<Long, Date> projectDateMap = filter.stream().collect(Collectors.toMap(TaskBoardDTO::getProjectId, TaskBoardDTO::getProjectPlanEndDate, (v1, v2) -> v2));
            log.info("首页任务看板,任务id:{},执行人{}", filterIds, allMyStaffNameWithSelf);
            List<PersonDO> personDOList = personMapper.getPersons(Lists.newArrayList(allMyStaffNameWithSelf), filterIds, PersonTypeEnum.TASK_EXECUTOR.getCode());

            Map<String, List<HomePageSingleTaskWorkTimeVO>> taskWorkTimeOnePersonMap = new HashMap<>();
            Map<String, List<HomePageSingleTaskWorkTimeVO>> taskWorkTimePersonProjectMap = new HashMap<>();
            personDOList.forEach(a -> {
                TaskBoardDTO taskBoardDTO = taskMap.get(a.getMainId());
                HomePageSingleTaskWorkTimeVO taskWorkTimeVO = TaskCopier.INSTANCE.convert2HomePage(taskBoardDTO);
                taskWorkTimeVO.setExecutor(a.getUserName());
                taskWorkTimeVO.setExecutorId(a.getUserId());
                boolean delay = (taskBoardDTO.getActualEndDate() == null && new Date().after(taskBoardDTO.getPlanEndDate())) ||
                        (taskBoardDTO.getActualEndDate() != null && taskBoardDTO.getActualEndDate().after(taskBoardDTO.getPlanEndDate()));
                taskWorkTimeVO.setIsDelay(delay);
                //每人所有任务数
                taskWorkTimeOnePersonMap.computeIfAbsent(a.getUserId(), v -> new ArrayList<>()).add(taskWorkTimeVO);
                //每人每个项目中任务
                taskWorkTimePersonProjectMap.computeIfAbsent(a.getUserId() + "#" + taskBoardDTO.getProjectId(), v -> new ArrayList<>()).add(taskWorkTimeVO);
            });
            taskWorkTimeOnePersonMap.forEach((k, v) -> {
                HomePageSingleWorkTimeVO workTimeVO = new HomePageSingleWorkTimeVO();
                List<Long> projectIds = new ArrayList<>();
                List<HomePageSingleProjectWorkTimeVO> projectWorkTimeVOList = new ArrayList<>();
                v.forEach(a -> {
                    if (!projectIds.contains(a.getProjectId())) {
                        //计算每个人每个项目所有任务工时
                        HomePageSingleProjectWorkTimeVO projectWorkTimeVO = new HomePageSingleProjectWorkTimeVO();
                        List<HomePageSingleTaskWorkTimeVO> taskWorkTimeVOList = taskWorkTimePersonProjectMap.get(a.getExecutorId() + "#" + a.getProjectId());

                        BigDecimal planUseTime = taskWorkTimeVOList.stream().map(HomePageSingleTaskWorkTimeVO::getPlanUseTime)
                                .filter(Objects::nonNull).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                        //每个项目的任务按开始时间排序
                        taskWorkTimeVOList.sort(Comparator.comparing(HomePageSingleTaskWorkTimeVO::getStartDate));
                        projectWorkTimeVO.setProjectId(a.getProjectId());
                        projectWorkTimeVO.setProjectName(a.getProjectName());
                        projectWorkTimeVO.setTotalPlanUseTime(planUseTime);
                        projectWorkTimeVO.setTaskCount(taskWorkTimeVOList.size());
                        projectWorkTimeVO.setProjectPlanEndDate(projectDateMap.get(a.getProjectId()));
                        projectWorkTimeVO.setTaskWorkTimeVos(taskWorkTimeVOList);
                        projectWorkTimeVO.setCategory(a.getCategory());
                        projectIds.add(a.getProjectId());
                        projectWorkTimeVOList.add(projectWorkTimeVO);
                    }
                });
                BigDecimal planUseTime = projectWorkTimeVOList.stream().map(HomePageSingleProjectWorkTimeVO::getTotalPlanUseTime)
                        .filter(Objects::nonNull).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                //每个人的项目按项目计划结束时间倒序
                projectWorkTimeVOList.sort(Comparator.comparing(HomePageSingleProjectWorkTimeVO::getProjectPlanEndDate).reversed());
                int count = projectWorkTimeVOList.stream().map(HomePageSingleProjectWorkTimeVO::getTaskCount).reduce(Integer::sum).orElse(0);
                workTimeVO.setTotalPlanUseTime(planUseTime);
                workTimeVO.setTaskCount(count);
                workTimeVO.setProjectWorkTimeVos(projectWorkTimeVOList);
                workTimeVO.setExecutor(personMap.get(k));
                workTimeVO.setExecutorId(k);
                result.add(workTimeVO);
            });
            // 团队中无任务的人
            List<HomePageSingleWorkTimeVO> noneTaskList = allMyStaffNameWithSelf.stream()
                    .filter(a -> !taskWorkTimeOnePersonMap.containsKey(a))
                    .map(a -> {
                        HomePageSingleWorkTimeVO o = new HomePageSingleWorkTimeVO();
                        o.setExecutor(personMap.get(a));
                        o.setExecutorId(a);
                        o.setTotalPlanUseTime(BigDecimal.ZERO);
                        o.setTaskCount(0);
                        o.setProjectWorkTimeVos(Lists.emptyList());
                        return o;
                    }).collect(Collectors.toList());
            result.addAll(noneTaskList);
            return BaseResult.success(result);
        } else if (HomePageTabEnum.INDIVIDUAL.getCode().equals(req.getTabType())) {
            //没有任务,个人直接返回
            return BaseResult.success(Lists.emptyList());
        }
        //没有任务,团队返回人员信息
        List<HomePageSingleWorkTimeVO> result = allMyStaffNameWithSelf.stream().map(a -> {
            HomePageSingleWorkTimeVO o = new HomePageSingleWorkTimeVO();
            o.setExecutor(personMap.get(a));
            o.setExecutorId(a);
            o.setTotalPlanUseTime(BigDecimal.ZERO);
            o.setTaskCount(0);
            o.setProjectWorkTimeVos(Lists.emptyList());
            return o;
        }).collect(Collectors.toList());
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<List<HomePageGroupWorkTimeVO>> getGroupTaskWorkTimeBoard(HomePageTaskBoardReq req) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<BaseInfoResponse> users = innerUserPersonClient.getAllMyStaffWithSelfInfo(userInfo.getId(), false);
        if (CollectionUtils.isEmpty(users)) {
            log.warn("getGroupTaskWorkTimeBoard users are empty, userInfo: {}", userInfo);
            return BaseResult.success(Collections.emptyList());
        }
        Set<String> userIds = users.stream().map(BaseInfoResponse::getAccount).collect(Collectors.toSet());
        // 部门id、员工id非空取交集
        if (CollectionUtils.isNotEmpty(req.getDeptIds())) {
            Set<String> deptAllMyStaff = Sets.newHashSet();
            for (String deptId : req.getDeptIds()) {
                deptAllMyStaff.addAll(innerUserPersonClient.getByGroupIdNew(deptId));
            }
            userIds.retainAll(deptAllMyStaff);
        }
        if (CollectionUtils.isNotEmpty(req.getTeamMembers())) {
            userIds.retainAll(req.getTeamMembers());
        }
        if (userIds.isEmpty()) {
            log.warn("getGroupTaskWorkTimeBoard filtered users are empty, userInfo: {}", userInfo);
            return BaseResult.success(Collections.emptyList());
        }
        users.removeIf(u -> !userIds.contains(u.getAccount()));
        List<HomePageGroupWorkTimeVO> res = users.stream().map(u ->
                new HomePageGroupWorkTimeVO(u.getAccount(),
                        u.getAlias() + CommonConstant.JOIN_LINE + u.getName(),
                        new ArrayList<>())
        ).collect(Collectors.toList());
        List<TaskBoardDTO> tasks = listTasksSuitDateRange(req.getStartDate(), req.getEndDate(), userIds);
        if (tasks.isEmpty()) {
            return BaseResult.success(res);
        }
        List<Long> taskIds = tasks.stream().map(TaskBoardDTO::getId).collect(Collectors.toList());
        List<ProjectDO> projects = projectMapper.getByIds(tasks.stream().map(TaskBoardDTO::getProjectId).collect(Collectors.toSet()));
        Map<Long, ProjectDO> projectById = Maps.uniqueIndex(projects, ProjectDO::getId);
        List<PersonDO> persons = personMapper.getPersons(userIds, taskIds, PersonTypeEnum.TASK_EXECUTOR.getCode());
        ListMultimap<Long, PersonDO> personsByMainId = Multimaps.index(persons, PersonDO::getMainId);
        Map<String, HomePageGroupWorkTimeVO> resByExecutorId = Maps.uniqueIndex(res, HomePageGroupWorkTimeVO::getExecutorId);
        for (Date iterDate = req.getStartDate();
             iterDate.before(req.getEndDate());
             iterDate = DateUtil.getStartOfNextDay(iterDate)) {
            final Date iterStartDate = iterDate;
            Date iterEndDate = DateUtil.getEndOfDay(iterDate);
            List<TaskBoardDTO> currentDayTasks = tasks.stream()
                    .filter(task ->
                            DateUtil.haveOverlap(task.getStartDate(), task.getEndDate(), iterStartDate, iterEndDate))
                    .collect(Collectors.toList());
            Map<String, List<TaskBoardDTO>> tasksByPersonId = new HashMap<>();
            for (TaskBoardDTO currentDayTask : currentDayTasks) {
                for (PersonDO personTask : personsByMainId.get(currentDayTask.getId())) {
                    tasksByPersonId.computeIfAbsent(personTask.getUserId(), k -> new ArrayList<>());
                    tasksByPersonId.get(personTask.getUserId()).add(currentDayTask);
                }
            }
            for (Map.Entry<String, List<TaskBoardDTO>> userEntry : tasksByPersonId.entrySet()) {
                HomePageGroupWorkTimeVO userRes = resByExecutorId.get(userEntry.getKey());
                if (userRes == null) {
                    continue;
                }
                HomePageProjectWorkTimeVO userPerDateWorkTime = new HomePageProjectWorkTimeVO(iterDate, new ArrayList<>());
                userRes.getList().add(userPerDateWorkTime);
                ListMultimap<Long, TaskBoardDTO> tasksByProjectId = Multimaps.index(userEntry.getValue(), TaskBoardDTO::getProjectId);
                for (Long key : tasksByProjectId.keySet()) {
                    List<TaskBoardDTO> value = tasksByProjectId.get(key);
                    HomePageSingleProjectWorkTimeVO projectDateWorkTime = new HomePageSingleProjectWorkTimeVO();
                    projectDateWorkTime.setProjectId(key);
                    Optional.ofNullable(projectById.get(key)).ifPresent(p -> {
                        projectDateWorkTime.setProjectName(p.getName());
                        projectDateWorkTime.setProjectPlanEndDate(p.getPlanEndDate());
                        projectDateWorkTime.setCategory(p.getCategory());
                    });
                    userPerDateWorkTime.getProjectInfo().add(projectDateWorkTime);
                    projectDateWorkTime.setTaskWorkTimeVos(value.stream().map(
                            TaskCopier.INSTANCE::convert2HomePage
                    ).collect(Collectors.toList()));
                }
            }
        }
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<List<String>> getHolidays(HomePageHolidayReq homePageHolidayReq) {
        Date startDate = homePageHolidayReq.getStartDate();
        Date endDate = homePageHolidayReq.getEndDate();
        if (homePageHolidayReq.getStartDate().after(homePageHolidayReq.getEndDate())) {
            startDate = homePageHolidayReq.getEndDate();
            endDate = homePageHolidayReq.getStartDate();
        }
        List<String> holidays = elapsedTimeClient.getHolidays(startDate, endDate, true);
        return BaseResult.success(holidays);
    }

    public List<HomePageProjectBoardDTO> filterByDate(UserTypeEnum userType, Date startDate, Date endDate, List<HomePageProjectBoardDTO> list) {
        if (userType.equals(UserTypeEnum.PD)) {
            return list.stream().filter(e -> {
                Date nodeStart = DateUtil.min(e.getStartPlan(), e.getDemandInternalAudit(), e.getDemandConstrue(), e.getDemandConstrueReverse(), e.getUedAudit());
                Date nodeEnd = DateUtil.max(e.getStartPlan(), e.getDemandInternalAudit(), e.getDemandConstrue(), e.getDemandConstrueReverse(), e.getUedAudit());
                return DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);
            }).collect(Collectors.toList());
        } else if (userType.equals(UserTypeEnum.RD)) {
            return list.stream().filter(e -> {
                Date nodeStart = DateUtil.min(e.getTechnicalDetailReview(), e.getDevelopStart(), e.getSubmitTest());
                Date nodeEnd = DateUtil.max(e.getTechnicalDetailReview(), e.getDevelopStart(), e.getSubmitTest());
                return DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);
            }).collect(Collectors.toList());
        } else if (userType.equals(UserTypeEnum.QA)) {
            return list.stream().filter(e -> {
                Date nodeStart = DateUtil.min(e.getWriteTestCases(), e.getUseCaseReview());
                Date nodeEnd = DateUtil.max(e.getWriteTestCases(), e.getUseCaseReview());
                boolean filter = DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);

                nodeStart = DateUtil.min(e.getTestStart(), e.getPublishSimulate(), e.getPublishOfficial());
                nodeEnd = DateUtil.max(e.getTestStart(), e.getPublishSimulate(), e.getPublishOfficial());
                filter = filter || DateUtil.haveOverlap(nodeStart, nodeEnd, startDate, endDate);

                return filter;
            }).collect(Collectors.toList());
        } else {
            return Lists.emptyList();
        }
    }

    private List<TaskBoardDTO> listTasksSuitDateRange(Date startDate, Date endDate, Collection<String> staffIds) {
        // SQL 排除状态为 暂停、作废的任务
        List<TaskBoardDTO> taskBoardDTOList = taskMapper.getByDate(startDate, endDate, staffIds);
        Date current = new Date();
        taskBoardDTOList.forEach(a -> {
            if (a.getActualStartDate() == null) {
                a.setStartDate(a.getPlanStartDate());
                a.setEndDate(a.getPlanEndDate());
            } else if (a.getActualEndDate() != null) {
                //实际开始和结束都不为空
                a.setStartDate(a.getActualStartDate());
                a.setEndDate(a.getActualEndDate());
            } else if (a.getActualStartDate().after(a.getPlanEndDate())) {
                //实际开始不空,结束为空,实际开始大于计划结束时间
                a.setStartDate(a.getActualStartDate());
                a.setEndDate(current);
            } else {
                //实际开始不空,结束为空,实际开始小于计划结束时间
                a.setStartDate(a.getActualStartDate());
                a.setEndDate(a.getPlanEndDate());
            }
        });
        return taskBoardDTOList.stream()
                .filter(a -> a.getPlanStartDate() != null && a.getPlanEndDate() != null
                        && DateUtil.haveOverlap(a.getStartDate(), a.getEndDate(), startDate, endDate))
                .collect(Collectors.toList());
    }

}
