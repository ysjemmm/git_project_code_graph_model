package com.timevale.forward.service.manager;

import cn.hutool.core.collection.CollUtil;
import com.timevale.crm.custom.provider.facade.api.CustomWecomRoomTaskClient;
import com.timevale.crm.custom.provider.service.vo.WecomRoomTextMsgTaskRequest;
import com.timevale.forward.dal.entity.AutomationRuleDO;
import com.timevale.forward.dal.entity.BugOfflineDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.ActionTypeEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.BugOnlinePriorityEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.BugStatusEnum;
import com.timevale.forward.service.context.AutomationExecutionContext;
import com.timevale.forward.service.observer.event.BugOfflineRemindMsgEvent;
import com.timevale.forward.service.observer.event.BugOnlineRemindMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.Time;
import java.util.List;

@Component
@Slf4j
public class AutomationTaskExecutor {

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private CustomWecomRoomTaskClient customWecomRoomTaskClient;

    public void execute(AutomationExecutionContext context) {
        AutomationRuleDO rule = context.getRule();
        Object target = context.getTarget();

        log.info("执行自动化规则：{}，目标：{}", rule.getName(), target);

        if (ActionTypeEnum.SEND_NOTIFICATION.getCode().equals(rule.getActionType())) {
            sendNotification(rule, context);
        }

        if (ActionTypeEnum.EXECUTE_TASK.getCode().equals(rule.getActionType())) {
            sendNotificationToChatGroup(rule, context);
        }
    }

    private void sendNotificationToChatGroup(AutomationRuleDO rule, AutomationExecutionContext context) {
        if (BizTypeEnum.BUG_ONLINE.getCode().equals(rule.getBizType())) {
            BugOnlineDO bug = (BugOnlineDO) context.getTarget();
            if (StringUtils.isNotEmpty(bug.getCustomerId()) && StringUtils.isNotEmpty(bug.getGroupId())) {
                // 1. 调用通知服务：发送到钉钉群
                WecomRoomTextMsgTaskRequest request = new WecomRoomTextMsgTaskRequest();
                request.setTaskName(rule.getName());
                request.setCid(Long.valueOf(bug.getCustomerId()));
                request.setRoomId(bug.getGroupId());
                request.setRoomName(bug.getGroupName());
                request.setText(String.format(rule.getActionConfig(), bug.getId(), bug.getPhenomenonDesc()));
                // 00:00:00
                request.setExecuteStartTime(Time.valueOf("00:00:00"));
                // 23:59:59
                request.setExecuteEndTime(Time.valueOf("23:59:59"));
                request.setPriorityLevel(6);
                request.setBusinessType(3);
                request.setBusinessParam(String.valueOf(bug.getId()));

                log.info("推送客户群消息：{}", request);
                customWecomRoomTaskClient.pushTextContent(request);
            }
        }
    }

    private void sendNotification(AutomationRuleDO rule, AutomationExecutionContext context) {

        Integer bizType = rule.getBizType();

        // 1. 获取接收人
        List<String> receivers = context.getReceiverIds();
        if (CollUtil.isEmpty(receivers)) {
            log.warn("自动化规则：{}，目标：{}，没有接收人，不发送通知", rule.getName(), context.getTarget());
            return;
        }

        if (BizTypeEnum.BUG_ONLINE.getCode().equals(bizType)) {
            BugOnlineDO bug = (BugOnlineDO) context.getTarget();
            // 3. 调用通知服务：站内信 + 钉钉
            messageEventPublisher.publish(
                    new BugOnlineRemindMsgEvent(
                            this,
                            bug.getName(),
                            BugOnlineStatusEnum.getTextByCode(bug.getStatus()),
                            receivers,
                            bug.getId(),
                            BugOnlinePriorityEnum.getTextByCode(bug.getPriority())
                    )
            );
        } else if (BizTypeEnum.BUG_OFFLINE.getCode().equals(bizType)) {
            BugOfflineDO bug = (BugOfflineDO) context.getTarget();
            // 3. 调用通知服务：站内信 + 钉钉
            messageEventPublisher.publish(
                    new BugOfflineRemindMsgEvent(
                            this,
                            bug.getId(),
                            receivers,
                            bug.getName(),
                            BugStatusEnum.getTextByCode(bug.getStatus())
                    )
            );
        }
    }
}