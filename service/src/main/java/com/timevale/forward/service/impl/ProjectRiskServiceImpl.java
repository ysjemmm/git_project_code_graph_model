package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectRiskCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningSubmitTestDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningTaskDTO;
import com.timevale.forward.dal.dto.UpdateTimeDTO;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectRiskService;
import com.timevale.forward.facade.api.query.ProjectRiskQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.facade.api.result.ProjectRiskVO;
import com.timevale.forward.facade.api.result.UpdateTimeVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.DistributionCopier;
import com.timevale.forward.service.copy.ProjectRiskCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.observer.event.ProjectNodeActualDateUnInputMsgEvent;
import com.timevale.forward.service.observer.event.ProjectNodePlanDateUnInputMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RestService
public class ProjectRiskServiceImpl implements ProjectRiskService {

    @Resource
    private ProjectRiskMapper projectRiskMapper;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private ProjectRiskExplanationComponent projectRiskExplanationComponent;

    @Resource
    private HomePageRiskWarningComponent riskWarningComponent;

    @Resource
    private HomePageRiskWarningSubmitTestComponent riskWarningSubmitTestComponent;

    @Resource
    private HomePageRiskWarningTaskComponent riskWarningTaskComponent;

    @Resource
    private DistributionComponent distributionComponent;

    @Resource
    private ProjectRiskRecordMapper projectRiskRecordMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private MessageEventPublisher messageEventPublisher;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectRiskAddReq projectRiskAddReq) {
        ProjectRiskDO riskDO = ProjectRiskCopier.INSTANCE.convert(projectRiskAddReq);
        // 类型为其它
        riskDO.setType(ProjectRiskTypeEnum.OTHER.getCode());
        riskDO.setSign("");
        riskDO.setMainId(0L);
        projectRiskMapper.insert(riskDO);

        // 添加项目说明
        projectRiskExplanationComponent.add(riskDO.getId(), projectRiskAddReq.getExplanation());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(ProjectRiskModifyReq projectRiskModifyReq) {
        ProjectRiskDO riskDO = ProjectRiskCopier.INSTANCE.convert(projectRiskModifyReq);
        projectRiskMapper.update(riskDO);

        Integer status = riskDO.getStatus();
        if (ProjectRiskStatusEnum.INVALID.getCode().equals(status)) {
            // 新增风险说明
            String explanation = ProjectRiskExplanationEnum.INVALID.getText();
            projectRiskExplanationComponent.add(riskDO.getId() , explanation);
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectRiskVO> get(Long projectRiskId) {
        ProjectRiskDO riskDO = projectRiskMapper.selectById(projectRiskId);
        if(riskDO == null){
            throw new BaseBizRuntimeException("该项目风险不存在");
        }

        ProjectRiskVO riskVO = ProjectRiskCopier.INSTANCE.convert(riskDO);

        // 枚举填充
        riskVO.setTypeName(ProjectRiskTypeEnum.getTextByCode(riskVO.getType()));
        riskVO.setStatusName(ProjectRiskStatusEnum.getTextByCode(riskVO.getStatus()));

        return BaseResult.success(riskVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectRiskVO>> list(ProjectRiskQueryList projectRiskQueryList) {
        // 开始分页
        PageHelper.startPage(projectRiskQueryList.pageNum, projectRiskQueryList.pageSize, CommonConstant.PROJECT_RISK_ORDER_BY);

        ProjectRiskCondition condition = ProjectRiskCopier.INSTANCE.convert(projectRiskQueryList);
        List<ProjectRiskDO> riskDOList = projectRiskMapper.select(condition);
        List<ProjectRiskVO> riskVOList = riskDOList.stream().map(ProjectRiskCopier.INSTANCE::convert).collect(Collectors.toList());

        // 枚举描述
        for (ProjectRiskVO e : riskVOList) {
            e.setTypeName(ProjectRiskTypeEnum.getTextByCode(e.getType()));
            e.setStatusName(ProjectRiskStatusEnum.getTextByCode(e.getStatus()));
        }

        // 返回分页数据
        PageInfo<ProjectRiskDO> pageInfo = new PageInfo<>(riskDOList);
        PageQueryResult<ProjectRiskVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(riskVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> sync() {
        // 需要更新项目风险的项目
        List<Integer> statusList = new ArrayList<>();
        statusList.add(ProjectStatusEnum.WAITING.getCode());
        statusList.add(ProjectStatusEnum.PLANING.getCode());
        statusList.add(ProjectStatusEnum.DEVING.getCode());
        statusList.add(ProjectStatusEnum.TESTING.getCode());
        List<ProjectDO> projectDOList = projectMapper.getByStatus(statusList);
        List<Long> projectIdList = projectDOList.stream().map(ProjectDO::getId).collect(Collectors.toList());

        // 判空
        if(CollectionUtils.isEmpty(projectIdList)){
            return BaseResult.success(true);
        }

        // sql 端
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectIdList(projectIdList);
        List<ProjectRiskDO> pendingRiskList = riskDOList.stream().filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus())).collect(Collectors.toList());
        List<ProjectRiskDO> completeRiskList = riskDOList.stream().filter(e -> ProjectRiskStatusEnum.COMPLETE.getCode().equals(e.getStatus())).collect(Collectors.toList());

        // 数据分发端
        List<HomePageRiskWarningDTO> nodeDTOList = riskWarningComponent.getRiskWarningAll();
        List<HomePageRiskWarningTaskDTO> taskDTOList = riskWarningTaskComponent.getRiskWarningTaskAll();
        List<HomePageRiskWarningSubmitTestDTO> testDTOList = riskWarningSubmitTestComponent.getRiskWarningSubmitTestAll();

        // Map<Long, List<HomePageRiskWarningDTO>> riskWarningGroup = nodeDTOList.stream()
        //         .collect(Collectors.groupingBy(HomePageRiskWarningDTO::getProjectId));
        // nodeDTOList.clear();
        // riskWarningGroup.forEach((k, v) -> {
        //     Optional<HomePageRiskWarningDTO> max = v.stream()
        //             .filter(e -> ProjectRiskTypeEnum.NODE_OVERDUE.getCode().equals(e.getRiskType()))
        //             .max((a, b) -> {
        //                 int compare = b.getNodeActualDate().compareTo(a.getNodeActualDate());
        //                 if(compare == 0){
        //                     Integer aCode = ProjectNodeEnum.getCodeByName(a.getNodeName());
        //                     Integer bCode = ProjectNodeEnum.getCodeByName(b.getNodeName());
        //                     return aCode.compareTo(bCode);
        //                 }
        //                 return compare;
        //             });
        //     v.removeIf(e -> ProjectRiskTypeEnum.NODE_OVERDUE.getCode().equals(e.getRiskType()));
        //     max.ifPresent(v::add);
        //     nodeDTOList.addAll(v);
        // });

        // sql端 唯一id
        Map<String, ProjectRiskDO> pendingRiskMap = createUniqueMap(pendingRiskList);
        Map<String, ProjectRiskDO> completeRiskMap = createUniqueMap(completeRiskList);

        // 数据分发端 唯一id
        Map<String, Object> newRiskMap = new HashMap<>();
        newRiskMap.putAll(taskDTOList.stream().collect(Collectors.toMap(e -> e.getRiskType() + "-" + e.getProjectId() + "-" + e.getTaskId(), Function.identity(), (a, b) -> a)));
        newRiskMap.putAll(testDTOList.stream().collect(Collectors.toMap(e -> e.getRiskType() + "-" + e.getProjectId() + "-" + e.getTestBillId(), Function.identity(), (a, b) -> a)));
        newRiskMap.putAll(nodeDTOList.stream().collect(Collectors.toMap(e -> e.getRiskType() + "-" + e.getProjectId() + "-" + e.getNodeName(), Function.identity(), (a, b) -> a)));

        // 判断去重，更新完成处理的风险
        List<ProjectRiskDO> updateList = new ArrayList<>();

        // 待处理风险 - 如果还能查询到，则风险状态不变,更新逾期时间；否则说明风险已经处理
        pendingRiskMap.forEach((k, v) -> {
            if(newRiskMap.containsKey(k)){
                updateList.add(syncUpdateRisk(v, newRiskMap.get(k)));
            }else{
                updateList.add(syncCompleteRisk(v));
            }
        });
        // 已完成风险 - 如果还能查询到，则又成为了风险
        completeRiskMap.forEach((k, v) -> {
            if(newRiskMap.containsKey(k)){
                updateList.add(syncUpdateRisk(v, newRiskMap.get(k)));
            }
        });
        if(CollectionUtils.isNotEmpty(updateList)){
            updateList.forEach(e -> projectRiskMapper.update(e));
        }

        log.info("项目风险同步更新完成, 当前时间{}", new Date());

        // 增加新风险 - 如果库中不包含，则是新的风险
        List<ProjectRiskDO> addRiskDOList = new ArrayList<>();
        newRiskMap.forEach((k, v) -> {
            if(!pendingRiskMap.containsKey(k) && !completeRiskMap.containsKey(k)){
                addRiskDOList.add(syncAddRisk(v));
            }
        });
        if(CollectionUtils.isNotEmpty(addRiskDOList)){
            projectRiskMapper.batchInsert(addRiskDOList);
        }

        log.info("项目风险同步新增完成, 当前时间{}", new Date());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<UpdateTimeVO> projectRiskUpdateTime() {
        UpdateTimeDTO updateTimeDTO = distributionComponent.getUpdateDate();
        UpdateTimeVO updateTimeVO = DistributionCopier.INSTANCE.convert(updateTimeDTO);
        return BaseResult.success(updateTimeVO);
    }

    /**
     * 创建项目风险- <唯一id，本身>
     *
     * @param riskDOList 风险dolist
     */
    private Map<String, ProjectRiskDO> createUniqueMap(List<ProjectRiskDO> riskDOList){
        Map<String, ProjectRiskDO> result = new HashMap<>();
        result.putAll(riskDOList.stream()
                .filter(e -> ProjectRiskTypeEnum.SUBMIT_FAILURE.getCode().equals(e.getType()) || ProjectRiskTypeEnum.TASK_OVERDUE.getCode().equals(e.getType()))
                .collect(Collectors.toMap(e -> e.getType() + "-" + e.getProjectId() + "-" + e.getMainId(), Function.identity(), (a, b) -> a)));

        result.putAll(riskDOList.stream()
                .filter(e -> ProjectRiskTypeEnum.NODE_OVERDUE.getCode().equals(e.getType()) || ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode().equals(e.getType()))
                .collect(Collectors.toMap(e -> e.getType() + "-" + e.getProjectId() + "-" + e.getName(), Function.identity(), (a, b) -> a)));

        return result;
    }

    /**
     * 系统添加风险
     *
     * @param object 风险对象
     */
    private ProjectRiskDO syncAddRisk(Object object){
        ProjectRiskDO riskDO = new ProjectRiskDO();
        riskDO.setStatus(ProjectRiskStatusEnum.PENDING.getCode());

        if(object instanceof HomePageRiskWarningDTO){
            HomePageRiskWarningDTO nodeRisk = (HomePageRiskWarningDTO) object;
            riskDO.setProjectId(nodeRisk.getProjectId());
            riskDO.setMainId(nodeRisk.getNodeId());
            riskDO.setType(nodeRisk.getRiskType());
            riskDO.setName(nodeRisk.getNodeName());
            riskDO.setSign(nodeRisk.getOverdueDay());

        }else if(object instanceof HomePageRiskWarningTaskDTO){
            HomePageRiskWarningTaskDTO taskRisk = (HomePageRiskWarningTaskDTO) object;
            riskDO.setProjectId(taskRisk.getProjectId());
            riskDO.setMainId(taskRisk.getTaskId());
            riskDO.setType(taskRisk.getRiskType());
            riskDO.setName(taskRisk.getTaskName());
            riskDO.setSign(taskRisk.getOverdueTime());

        }else if(object instanceof HomePageRiskWarningSubmitTestDTO){
            HomePageRiskWarningSubmitTestDTO testRisk = (HomePageRiskWarningSubmitTestDTO) object;
            riskDO.setProjectId(testRisk.getProjectId());
            riskDO.setMainId(testRisk.getTestBillId());
            riskDO.setType(testRisk.getRiskType());
            riskDO.setName(testRisk.getTestBillName());
            riskDO.setSign(ProjectRiskSignEnum.SUBMIT_FAILURE.getText());
        }

        return riskDO;
    }

    /**
     * 项目风险完成 - 任务、节点 计算逾期时间
     *
     * @param riskDO 风险DO
     */
    public ProjectRiskDO syncCompleteRisk(ProjectRiskDO riskDO){
        String sign = "";

        if(ProjectRiskTypeEnum.TASK_OVERDUE.getCode().equals(riskDO.getType())){
            TaskDO taskDO = taskMapper.getById(riskDO.getMainId());
            Date planEndDate = taskDO.getPlanEndDate();
            Date actualEndDate = taskDO.getActualEndDate();

            // 逾期时间可以为负数
            Long result = 0L;

            int compare;
            // 保险验证
            if(actualEndDate == null){
                actualEndDate = new Date();
            }
            if(planEndDate == null){
                compare = 0;
            }else{
                compare = actualEndDate.compareTo(planEndDate);
            }

            if(compare > 0){
                result = elapsedTimeClient.getElapsedTime(planEndDate, actualEndDate);
            }else if(compare < 0){
                result = - elapsedTimeClient.getElapsedTime(actualEndDate, planEndDate);
            }
            BigDecimal elapsedTime = new BigDecimal(result.toString());
            elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.ONE_HOUR  / DateFormatConst.ONE_SECOND), 2, RoundingMode.HALF_UP);

            sign = elapsedTime.toString();

        }else if(ProjectRiskTypeEnum.NODE_OVERDUE.getCode().equals(riskDO.getType())
                || ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode().equals(riskDO.getType())){
            ProjectNodeDO nodeDO = projectNodeMapper.getByName(riskDO.getProjectId(), riskDO.getName());
            if(nodeDO == null){
                // 如果节点被删除则设定为0
                sign = "0";
            }else {
                Date planDate = nodeDO.getPlanDate();
                Date actualDate = nodeDO.getActualDate();

                // 逾期时间可以为负数
                Long result = 0L;

                int compare;
                // 保险验证
                if(planDate == null || actualDate == null){
                    compare = 0;
                }else{
                    compare = actualDate.compareTo(planDate);
                }

                if(compare > 0){
                    result = elapsedTimeClient.getElapsedTime(planDate, actualDate);
                }else if(compare < 0){
                    result = - elapsedTimeClient.getElapsedTime(actualDate, planDate);
                }
                BigDecimal elapsedTime = new BigDecimal(result.toString());
                elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
                sign = elapsedTime.toString();
            }
        }

        ProjectRiskDO result = new ProjectRiskDO();
        result.setId(riskDO.getId());
        result.setSign(sign);
        result.setStatus(ProjectRiskStatusEnum.COMPLETE.getCode());

        return result;
    }

    /**
     * 项目风险更新
     *
     * @param object 风险DTO
     */
    private ProjectRiskDO syncUpdateRisk(ProjectRiskDO riskDO, Object object){
        ProjectRiskDO result = new ProjectRiskDO();
        result.setId(riskDO.getId());
        result.setStatus(ProjectRiskStatusEnum.PENDING.getCode());

        // 如果产生状态变化，则修改更新时间
        if(ProjectRiskStatusEnum.PENDING.getCode().equals(riskDO.getStatus())){
            result.setModifyDate(riskDO.getModifyDate());
        }

        if(object instanceof HomePageRiskWarningDTO){
            HomePageRiskWarningDTO nodeRisk = (HomePageRiskWarningDTO) object;
            result.setSign(nodeRisk.getOverdueDay());
        }else if(object instanceof HomePageRiskWarningTaskDTO){
            HomePageRiskWarningTaskDTO taskRisk = (HomePageRiskWarningTaskDTO) object;
            result.setSign(taskRisk.getOverdueTime());
        }

        return result;
    }

    @Override
    public BaseResult<Boolean> syncRiskRecord() {
        log.info("项目节点逾期未录入,任务开始");
        //未处理的逾期未录入风险
        List<ProjectRiskDO> projectRiskDOList = projectRiskMapper.selectByStatusType(ProjectRiskStatusEnum.PENDING.getCode(), ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode());
        if(CollectionUtils.isEmpty(projectRiskDOList)){
            log.info("没有需要处理逾期未录入的风险");
            return BaseResult.success(true);
        }
        List<Long> projectIds = projectRiskDOList.stream().map(ProjectRiskDO::getProjectId).distinct().collect(Collectors.toList());
        List<ProjectDO> projectDOList = projectMapper.getByIds(projectIds);

        List<ProjectDO> filterProject = projectDOList.stream().filter(a -> !ProjectStatusEnum.terminated(a.getStatus())).collect(Collectors.toList());
        List<Long> filterIds = filterProject.stream().map(ProjectDO::getId).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(filterIds)){
            log.info("没有需要处理的项目");
            return BaseResult.success(true);
        }
        Map<Long, ProjectDO> projectMap = filterProject.stream().collect(Collectors.toMap(ProjectDO::getId, b -> b, (v1, v2) -> v2));
        Map<Long, List<PersonDO>> pdMap = personMapper.get(filterIds, PersonTypeEnum.PROJECT_PD.getCode())
                .stream().collect(Collectors.groupingBy(PersonDO::getMainId));
        //过滤暂停,作废,发布,状态风险
        List<ProjectRiskDO> filterRiskList = projectRiskDOList.stream().filter(a -> filterIds.contains(a.getProjectId())).collect(Collectors.toList());

        List<ProjectNodeDO> projectNodeDOList = projectNodeMapper.selectByProjectIdListFilterDate(filterIds);
        //待处理的节点
        Map<String, ProjectNodeDO> projectNodeMap = projectNodeDOList.stream().collect(Collectors.toMap(a -> a.getProjectId() + "-" + a.getName(), a->a, (v1, v2) -> v2));
        //已经处理过的记录
        List<ProjectRiskRecordDO> projectRiskRecordDOList = projectRiskRecordMapper.get(null,ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode());
        Map<String, ProjectRiskRecordDO> riskRecordMap = projectRiskRecordDOList.stream()
                .collect(Collectors.toMap(a -> a.getMainId() + "-" + a.getName() + "-" + a.getReceiveManId(), b -> b, (v1, v2) -> v2));
        List<ProjectRiskRecordDO> result = new ArrayList<>();
        for (ProjectRiskDO projectRiskDO : filterRiskList) {
            if (!projectMap.containsKey(projectRiskDO.getProjectId())) {
                continue;
            }
            ProjectDO projectDO = projectMap.get(projectRiskDO.getProjectId());
            Integer code = ProjectNodeEnum.getCodeByName(projectRiskDO.getName());
            String dateKey = projectRiskDO.getProjectId() + "-" + projectRiskDO.getName();
            ProjectNodeDO nodeDO = projectNodeMap.get(dateKey);
            if(nodeDO!=null){
                if (code < 30) {
                    //需求规划阶段,消息接收人找pd
                    if (!pdMap.containsKey(projectRiskDO.getProjectId())) {
                        continue;
                    }
                    for (PersonDO personDO : pdMap.get(projectRiskDO.getProjectId())) {
                        String key = projectRiskDO.getProjectId() + "-" + projectRiskDO.getName() + "-" + personDO.getUserId();
                        if (!riskRecordMap.containsKey(key)) {
                            result.add(createProjectRiskRecordDO(projectRiskDO.getProjectId(), projectRiskDO.getName(), personDO.getUserName(), personDO.getUserId()));
                            send(projectRiskDO.getProjectId(),projectRiskDO.getName(),projectDO.getName(),nodeDO.getPlanDate(),personDO.getUserId());
                        }
                    }

                } else {
                    //其他阶段,消息接收人找pm
                    String key = projectRiskDO.getProjectId() + "-" + projectRiskDO.getName() + "-" + projectDO.getPmId();
                    if (!riskRecordMap.containsKey(key)) {
                        result.add(createProjectRiskRecordDO(projectRiskDO.getProjectId(), projectRiskDO.getName(), projectDO.getPmName(), projectDO.getPmId()));
                        send(projectRiskDO.getProjectId(),projectRiskDO.getName(),projectDO.getName(),nodeDO.getPlanDate(),projectDO.getPmId());
                    }

                }
            }

        }
        if(CollectionUtils.isNotEmpty(result)){
            projectRiskRecordMapper.batchInsert(result);
        }
        log.info("项目节点逾期未录入,任务结束,共计: {}条", result.size());
        return BaseResult.success(true);
    }

    private ProjectRiskRecordDO createProjectRiskRecordDO(Long projectId, String name, String receiveMan, String receiveManId) {
        ProjectRiskRecordDO riskRecordDO = new ProjectRiskRecordDO();
        riskRecordDO.setName(name);
        riskRecordDO.setMainId(projectId);
        riskRecordDO.setType(ProjectRiskTypeEnum.NODE_ENTRY_OVERDUE.getCode());
        riskRecordDO.setReceiveMan(receiveMan);
        riskRecordDO.setReceiveManId(receiveManId);
        return riskRecordDO;
    }

    private void send(Long projectId,String nodeName,String projectName, Date planDate, String receiveManId) {
        if(planDate==null){
            messageEventPublisher.publish(new ProjectNodePlanDateUnInputMsgEvent(
                    this,
                    projectId,
                    receiveManId,
                    nodeName,
                    projectName
            ));
        }else{
            messageEventPublisher.publish(new ProjectNodeActualDateUnInputMsgEvent(
                    this,
                    projectId,
                    receiveManId,
                    nodeName,
                    DateUtil.parseToString(planDate, DateStyle.YYYY_MM_DD),
                    projectName
            ));
        }
    }

}
