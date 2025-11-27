package com.timevale.forward.service.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.timevale.forward.dal.dao.AutomationRuleMapper;
import com.timevale.forward.dal.entity.AutomationRuleDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.ReceiverTypeEnum;
import com.timevale.forward.model.enums.TriggerTypeEnum;
import com.timevale.forward.service.context.AutomationExecutionContext;
import com.timevale.forward.service.observer.event.OnlineBugStatusChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class BugOnlineStatusChangeEventListener {

    @Resource
    private AutomationRuleMapper automationRuleMapper;

    @Resource
    private AutomationTaskExecutor executor;

    @EventListener
    public void handleBugStatusChange(OnlineBugStatusChangeEvent event) {
        BugOnlineDO bug = event.getBugOnline();
        String oldStatus = event.getOldStatus();
        String newStatus = event.getNewStatus();

        log.info("检测到 Bug 状态变更：ID={}，从 {} 变更为 {}", bug.getId(), oldStatus, newStatus);

        // 1. 查询所有启用的、针对该业务类型的、触发类型为 status_change 的规则
        List<AutomationRuleDO> rules = automationRuleMapper
                .findByBizTypeAndTriggerTypeAndIsActive(BizTypeEnum.BUG_ONLINE.getCode(), TriggerTypeEnum.STATUS_CHANGE.getCode());

        for (AutomationRuleDO rule : rules) {
            // 2. 解析状态条件 JSON：{ "from": "处理中", "to": "已关闭" }
            String statusConditionJson = rule.getStatusCondition();
            try {
                JSONObject condition = JSON.parseObject(statusConditionJson);
                String fromStatus = condition.getString("from");
                String toStatus = condition.getString("to");

                // 3. 判断是否匹配：oldStatus == from && newStatus == to
                if (fromStatus.equals(oldStatus) && toStatus.equals(newStatus)) {
                    log.info("规则 [{}] 匹配成功，准备执行", rule.getName());

                    // 4. 构造执行上下文
                    AutomationExecutionContext context = new AutomationExecutionContext();
                    context.setRule(rule);
                    context.setTarget(bug);
                    // 默认解决人
                    context.setReceiverIds(getReceivers(rule, bug));

                    // 5. 执行动作
                    executor.execute(context);
                }
            } catch (Exception e) {
                log.error("解析或匹配状态条件失败，规则ID={}", rule.getId(), e);
            }
        }
    }

    private List<String> getReceivers(AutomationRuleDO rule, BugOnlineDO bug) {
        if (ReceiverTypeEnum.OPERATOR.getCode().equals(rule.getReceiverType())) {
            return Collections.singletonList(bug.getOperatorId());
        }
        if (ReceiverTypeEnum.CUSTOM_USERS.getCode().equals(rule.getReceiverType())) {
            return JSON.parseArray(rule.getReceiverIds(), String.class);
        }
        return Collections.emptyList();
    }
}