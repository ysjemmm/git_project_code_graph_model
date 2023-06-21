package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.DataCorrectService;
import com.timevale.forward.facade.api.request.ProjectNodeModifyReq;
import com.timevale.forward.facade.api.result.BizRecordVO;
import com.timevale.forward.facade.api.result.BizStatusOperatorVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BugOnlineStatusOperatorComponent;
import com.timevale.forward.service.component.ProjectEvaluateComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class DataCorrectServiceImpl implements DataCorrectService {
    @Resource
    private BizChangeLogMapper bizChangeLogMapper;
    @Resource
    private BizRecordMapper bizRecordMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectNodeMapper projectNodeMapper;
    @Resource
    private TestBillMapper testBillMapper;
    @Resource
    private BizDemandMapper bizDemandMapper;
    @Resource
    private BizDemandComponent bizDemandComponent;
    @Resource
    private ProjectNodeComponent projectNodeComponent;
    @Resource
    private TroubleTicketMapper troubleTicketMapper;
    @Resource
    private BugOnlineStatusOperatorMapper bugOnlineStatusOperatorMapper;
    @Resource
    private BugLogMapper bugLogMapper;
    @Resource
    private BugOnlineStatusOperatorComponent bugOnlineStatusOperatorComponent;
    @Resource
    private SearchConditionMapper searchConditionMapper;
    @Resource
    private ProjectMilestoneMapper milestoneMapper;
    @Resource
    private ProjectMilestoneActionMapper milestoneActionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> nodeStatusUpdate() {
        List<Long> projectIdList = projectMapper.getAllId();

        projectIdList.forEach(e -> {
            // 查询项目节点
            List<ProjectNodeDO> nodeDOList = projectNodeComponent.get(e);

            // 如果节点为空则状态设为待启动
            Integer nodeStatus;
            if (org.apache.commons.collections.CollectionUtils.isEmpty(nodeDOList)) {
                nodeStatus = ProjectNodeStatusEnum.READY_START.getCode();
            } else {
                nodeStatus = projectNodeComponent.getStatus(nodeDOList);
            }
            // 更新项目节点状态
            ProjectDO projectDO = new ProjectDO();
            projectDO.setId(e);
            projectDO.setNodeStatus(nodeStatus);
            projectMapper.updateNodeStatus(projectDO);
        });

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> bizDemandProjectEndDateUpdate() {
        List<BizDemandDO> bizDemandDOList = bizDemandMapper.selectAll();
        bizDemandDOList = bizDemandDOList.stream().filter(e -> !e.getIsDeleted()).collect(Collectors.toList());

        for (BizDemandDO e : bizDemandDOList) {
            Date projectEndDate = bizDemandComponent.getProjectEndDate(e.getId());
            if (projectEndDate != null) {
                bizDemandMapper.updateDate(e.getId(), projectEndDate, DateUtil.getMonth(projectEndDate) - 1);
            }
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateNodeDate(ProjectNodeModifyReq req) {
        projectNodeMapper.updateActualDateById(req.getId(), req.getActualDate());
        ProjectNodeDO projectNodeDO = projectNodeMapper.getById(req.getId());
        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setProjectId(projectNodeDO.getProjectId());
        testBillDO.setDelayDay(0);
        testBillMapper.updateDelayDay(testBillDO, true);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> troubleTicketTime() {
        List<TroubleTicketDO> troubleTicketDOS = troubleTicketMapper.selectAll();

        // 只保存仅有的时间
        troubleTicketDOS = troubleTicketDOS.stream()
                .filter(e -> e.getOccurrenceTime() != null && e.getRestoreTime() != null)
                .collect(Collectors.toList());

        for (TroubleTicketDO e : troubleTicketDOS) {
            Date occurrenceTime = e.getOccurrenceTime();
            Date restoreTime = e.getRestoreTime();

            long occurrenceTimeTime = occurrenceTime.getTime();
            long restoreTimeTime = restoreTime.getTime();

            long differ = Math.max(restoreTimeTime - occurrenceTimeTime, 0L);
            long result = differ / DateFormatConst.ONE_MINUTE;
            BigDecimal durationTime = BigDecimal.valueOf(result);

            log.info("[DataCorrectServiceImpl][troubleTicketTime]更新故障单{}持续时间{}", e.getId(), durationTime);
            troubleTicketMapper.updateDurationTime(e.getId(), durationTime);
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> bugOnlineCloseStatusOperatorInit(Integer count) {
        List<Long> bugOnlineIds = bugOnlineStatusOperatorMapper.selectInitInfo(count);
        if (CollectionUtils.isEmpty(bugOnlineIds)) {
            BaseResult.success(false);
        }
        bugOnlineIds.forEach(a -> {
            List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(a, BugLogTypeEnum.ONLINE.getCode(), true)
                    .stream().filter(b -> BugOnlineStatusEnum.CLOSE.getText().equals(b.getNewValue())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bugLogDOList)) {
                bugLogDOList.sort(Comparator.comparing(BugLogDO::getCreateDate).reversed());
                BugLogDO bugLogDO = bugLogDOList.get(0);
                BugOnlineStatusOperatorDO bugOnlineStatusOperatorDO = new BugOnlineStatusOperatorDO();
                bugOnlineStatusOperatorDO.setBugOnlineId(a);
                bugOnlineStatusOperatorDO.setOperator(bugLogDO.getCreateMan());
                bugOnlineStatusOperatorDO.setStatus(BugOnlineStatusEnum.CLOSE.getCode());
                bugOnlineStatusOperatorDO.setOperatorId(bugLogDO.getCreateManId());
                bugOnlineStatusOperatorComponent.add(bugOnlineStatusOperatorDO);
            }

        });
        return BaseResult.success(true);
    }

    @Resource
    private EvaluateDimensionMapper dimensionMapper;
    @Resource
    private ProjectEvaluateMapper evaluateMapper;
    @Resource
    private ProjectEvaluateComponent projectEvaluateComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateEval() {
        List<ProjectDO> projectDOList = projectMapper.getThisYear();

        for (ProjectDO projectDO : projectDOList) {
            Long projectId = projectDO.getId();
            Integer kind = projectDO.getKind();

            List<EvaluateDimensionDO> dimensionDOList = dimensionMapper.selectByKindDate(kind, new Date());
            List<Long> dimensionIdList = dimensionDOList.stream().map(BaseDO::getId).collect(Collectors.toList());

            List<ProjectEvaluateDO> byProjectId = evaluateMapper.getByProjectId(projectId);
            List<Long> oldDimensionIdList = byProjectId.stream().map(ProjectEvaluateDO::getEvaluateDimensionId).collect(Collectors.toList());

            dimensionIdList.removeAll(oldDimensionIdList);
            if (CollUtil.isNotEmpty(dimensionIdList)) {
                evaluateMapper.batchInsert(projectId, dimensionIdList);
            }

            projectEvaluateComponent.syncMember(projectId);
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateBugOnlineQuery() {
        String reasonReg = ",\"reasons\":\\[[\\d,\\,]+\\],";
        String dismissReg = ",\"dismissCauseList\":\\[[\\d,\\,]+\\],";

        String reasonReplace = ",\"reasons\":[],";
        String dismissReplace = ",\"dismissCauseList\":[],";

        Pattern reasonPattern = Pattern.compile(reasonReg);
        Pattern dismissPattern = Pattern.compile(dismissReg);

        List<SearchConditionDO> conditionDOs = searchConditionMapper.getByModel(50);
        for (SearchConditionDO conditionDO : conditionDOs) {
            String content = conditionDO.getContent();

            boolean needUpdate = false;

            Matcher reasonMatcher = reasonPattern.matcher(content);
            if (reasonMatcher.find()) {
                needUpdate = true;
                content = reasonMatcher.replaceAll(reasonReplace);
            }

            Matcher dismissMatcher = dismissPattern.matcher(content);
            if (dismissMatcher.find()) {
                needUpdate = true;
                content = dismissMatcher.replaceAll(dismissReplace);
            }

            if (needUpdate) {
                searchConditionMapper.updateContent(conditionDO.getId(), content);
            }
        }
        return BaseResult.success(true);
    }

    @Resource
    private TaskMapper taskMapper;

    /**
     * 刷新里程碑行动
     *
     * @return {@link BaseResult}<{@link Void}>
     */
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Void> milestoneAction() {
        List<ProjectMilestone> milestones = milestoneMapper.selectAll();

        // 刷新里程碑时间
        for (ProjectMilestone milestone : milestones) {
            if (MilestoneTypeEnum.TASK.getCode().equals(milestone.getType())) {
                TaskDO task = taskMapper.getById(milestone.getRelationId());
                milestoneMapper.updateDate(milestone.getId(), task.getPlanStartDate(), task.getPlanEndDate());
            } else {
                ProjectDO project = projectMapper.get(milestone.getRelationId());
                milestoneMapper.updateDate(milestone.getId(), project.getPlanStartDate(), project.getPlanEndDate());
            }
        }

        // 添加里程碑行动
        List<ProjectMilestoneActionDO> actions = milestones.stream()
                .map(e -> {
                    ProjectMilestoneActionDO actionDO = new ProjectMilestoneActionDO();
                    actionDO.setType(e.getType());
                    actionDO.setMilestoneId(e.getId());
                    actionDO.setRelationId(e.getRelationId());
                    return actionDO;
                }).collect(Collectors.toList());

        milestoneActionMapper.batchAdd(actions);

        return BaseResult.success();
    }

    private final String fieldStatus = "需求解决状态";
    private final String fieldOperator = "需求接收人";

    @Override
    public BaseResult<Void> bizRecord(Long mainId) {
        List<BizChangeLogDO> logs = bizChangeLogMapper.list(mainId, 4);

        Map<Long, List<BizChangeLogDO>> logGroups = logs.stream()
                .filter(e -> Objects.equals(mainId, e.getMainId()))
                .filter(e -> e.getCreateDate().compareTo(DateUtil.parseToDate("2023-01-01")) >= 0)
                .filter(e -> fieldStatus.equals(e.getField()) || fieldOperator.equals(e.getField()))
                .sorted(Comparator.comparing(BaseDO::getCreateDate))
                .collect(Collectors.groupingBy(BizChangeLogDO::getMainId));
        logGroups.forEach(this::solve);

        return BaseResult.success();
    }

    private void solve(Long mainId, List<BizChangeLogDO> logs) {
        BizDemandDO bizDemand = bizDemandMapper.get(mainId);

        // 初始数据状态
        Date date = bizDemand.getCreateDate();
        String status = BizDemandStatusEnum.EVALUATE.getText();
        String operator = logs.stream()
                .filter(e -> fieldOperator.equals(e.getField()))
                .sorted(Comparator.comparing(BaseDO::getCreateDate))
                .map(BizChangeLogDO::getOldValue)
                .findFirst()
                .orElseGet(bizDemand::getReceiveMan);

        List<BizRecordDO> records = new ArrayList<>();
        List<BizStatusOperatorVO> operators = CollUtil.newArrayList(new BizStatusOperatorVO(operator, date));

        for (BizChangeLogDO log : logs) {
            if (fieldOperator.equals(log.getField())) {
                operators.add(new BizStatusOperatorVO(log.getNewValue(), log.getCreateDate()));
            } else {
                if (!Objects.equals(status, log.getNewValue())) {
                    records.add(createRecord(mainId, status, date, operators));
                }

                date = log.getCreateDate();
                status = log.getNewValue();
                operators = CollUtil.newArrayList(CollUtil.getFirst(operators));
            }
        }
        records.add(createRecord(mainId, status, date, operators));

        bizRecordMapper.batchInsert(records);
    }

    private BizRecordDO createRecord(Long mainId, String status, Date date, List<BizStatusOperatorVO> operators) {
        operators.sort(Comparator.comparing(BizStatusOperatorVO::getOperatorDate).reversed());

        BizRecordVO recordVO = new BizRecordVO();
        recordVO.setStatus(status);
        recordVO.setCreateDate(date);
        recordVO.setStatusOperatorVOList(operators);

        BizRecordDO record = new BizRecordDO();
        record.setMainId(mainId);
        record.setCreateDate(date);
        record.setRecord(JSON.toJSONString(recordVO));
        record.setMainType(BizChangeLogTypeEnum.BIZ_DEMAND.getCode());

        return record;
    }
}
