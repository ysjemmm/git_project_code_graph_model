package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningSubmitTestDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningTaskDTO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.client.ProjectRiskService;
import com.timevale.forward.facade.api.query.ProjectRiskQueryList;
import com.timevale.forward.facade.api.request.ProjectRiskAddReq;
import com.timevale.forward.facade.api.request.ProjectRiskModifyReq;
import com.timevale.forward.facade.api.result.ProjectRiskVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.HomePageRiskWarningComponent;
import com.timevale.forward.service.component.HomePageRiskWarningSubmitTestComponent;
import com.timevale.forward.service.component.HomePageRiskWarningTaskComponent;
import com.timevale.forward.service.component.ProjectRiskExplanationComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectRiskCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateFormatConst;
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
    ProjectRiskMapper projectRiskMapper;

    @Resource
    ElapsedTimeClient elapsedTimeClient;

    @Resource
    ProjectNodeMapper projectNodeMapper;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    TaskMapper taskMapper;

    @Resource
    ProjectRiskExplanationComponent projectRiskExplanationComponent;

    @Resource
    HomePageRiskWarningComponent riskWarningComponent;

    @Resource
    HomePageRiskWarningSubmitTestComponent riskWarningSubmitTestComponent;

    @Resource
    HomePageRiskWarningTaskComponent riskWarningTaskComponent;


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

        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectId(projectRiskQueryList.getProjectId());
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
                    result = elapsedTimeClient.getElapsedTimeAllDay(planDate, actualDate);
                }else if(compare < 0){
                    result = - elapsedTimeClient.getElapsedTimeAllDay(actualDate, planDate);
                }
                BigDecimal elapsedTime = new BigDecimal(result.toString());
                elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.ONE_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
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

}
