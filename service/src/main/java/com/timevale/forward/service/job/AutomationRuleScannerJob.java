package com.timevale.forward.service.job;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.AutomationRuleMapper;
import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.entity.AutomationRuleDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.ReceiverTypeEnum;
import com.timevale.forward.model.enums.TriggerTypeEnum;
import com.timevale.forward.service.context.AutomationExecutionContext;
import com.timevale.forward.service.manager.AutomationTaskExecutor;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@JobHandler(value = "automationRuleScannerJob")
@Slf4j
public class AutomationRuleScannerJob extends IJobHandler {

    @Resource
    private AutomationRuleMapper automationRuleMapper;

    @Resource
    private BugOnlineMapper bugOnlineMapper;

    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Resource
    private AutomationTaskExecutor executor;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        log.info("开始扫描时间触发类型的自动化规则...");
        List<AutomationRuleDO> rules = automationRuleMapper.findByTriggerTypeAndIsEnabled(TriggerTypeEnum.SCHEDULED.getCode(), true);

        for (AutomationRuleDO rule : rules) {
            checkAndTriggerTimeRule(rule);
        }
        return ReturnT.SUCCESS;
    }

    private void checkAndTriggerTimeRule(AutomationRuleDO rule) {
        // 1. 解析规则中的时间字段、提前提醒天数、提醒时间点
        // expect_resolve_time
        String timeFieldName = rule.getTimeField();
        // 如 1
        int remindDays = rule.getRemindDaysBefore();
        // "09:00"
        String remindTimeStr = rule.getRemindTime();
        Integer bizType = rule.getBizType();

        LocalTime remindTime = LocalTime.parse(remindTimeStr);

        if (BizTypeEnum.BUG_ONLINE.getCode().equals(bizType)) {
            // 2. 查询所有符合条件的业务对象，线上 Bug
            List<BugOnlineDO> bugs;
            if (rule.getIsOverdueNotify()) {
                bugs = bugOnlineMapper.findBugsOverdue(timeFieldName);
            } else {
                bugs = bugOnlineMapper.findBugsApproachingResolveTime(timeFieldName, remindDays, remindTime);
            }
            for (BugOnlineDO bug : bugs) {
                // 3. 构造执行上下文，触发执行器
                AutomationExecutionContext context = buildBugOnlineContext(rule, bug);
                executor.execute(context);
            }
        } else if (BizTypeEnum.BUG_OFFLINE.getCode().equals(bizType)) {
            // 4. 查询所有符合条件的业务对象，线下 Bug
            List<BugOfflineDO> offlineBugs;
            if (rule.getIsOverdueNotify()) {
                offlineBugs = bugOfflineMapper.findBugsOverdue(timeFieldName);
            } else {
                offlineBugs = bugOfflineMapper.findBugsApproachingResolveTime(timeFieldName, remindDays, remindTime);
            }

            for (BugOfflineDO bug : offlineBugs) {
                // 5. 构造执行上下文，触发执行器
                AutomationExecutionContext context = buildBugOfflineContext(rule, bug);
                executor.execute(context);
            }
        }
    }

    private AutomationExecutionContext buildBugOnlineContext(AutomationRuleDO rule, BugOnlineDO bug) {
        AutomationExecutionContext context = new AutomationExecutionContext();
        context.setRule(rule);
        context.setTarget(bug);
        context.setReceiverIds(getBugOnlineReceivers(rule, bug));
        return context;
    }

    private AutomationExecutionContext buildBugOfflineContext(AutomationRuleDO rule, BugOfflineDO bug) {
        AutomationExecutionContext context = new AutomationExecutionContext();
        context.setRule(rule);
        context.setTarget(bug);
        context.setReceiverIds(getBugOfflineReceivers(rule, bug));
        return context;
    }

    private List<String> getBugOnlineReceivers(AutomationRuleDO rule, BugOnlineDO bug) {
        if (ReceiverTypeEnum.OPERATOR.getCode().equals(rule.getReceiverType())) {
            // 从业务对象中拿解决人
            return Collections.singletonList(bug.getOperatorId());
        }
        // 支持自定义
        if (ReceiverTypeEnum.CUSTOM_USERS.getCode().equals(rule.getReceiverType())) {
            // 自定义,["user1", "user2"]
            return JSON.parseArray(rule.getReceiverIds(), String.class);
        }
        return Collections.emptyList();
    }

    private List<String> getBugOfflineReceivers(AutomationRuleDO rule, BugOfflineDO bug) {
        if (ReceiverTypeEnum.OPERATOR.getCode().equals(rule.getReceiverType())) {
            // 从业务对象中拿解决人
            return Collections.singletonList(bug.getOperatorId());
        }
        if (ReceiverTypeEnum.CUSTOM_USERS.getCode().equals(rule.getReceiverType())) {
            // 自定义,["user1", "user2"]
            return JSON.parseArray(rule.getReceiverIds(), String.class);
        }
        // 支持自定义
        return Collections.emptyList();
    }
}