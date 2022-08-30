package com.timevale.forward.service.mq.listener;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.model.enums.MessageTagEnum;
import com.timevale.forward.service.component.ProjectFlowComponent;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.component.impl.ProductDemandDescFlowComponent;
import com.timevale.forward.service.mq.dto.WorkflowBody;
import com.timevale.forward.service.mq.handler.MqMessageHandler;
import com.timevale.framework.mq.client.consumer.Listener;
import com.timevale.framework.mq.client.consumer.ReceiveResult;
import com.timevale.framework.mq.client.producer.Msg;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xingyun
 * @date 2022/05/17 19:54
 */
@Slf4j
@Component
public class MqListener implements Listener {

    @Resource
    private ProjectFlowComponent projectFlowComponent;

    @Resource
    private TrackEventComponent trackEventComponent;

    @Resource
    private ProjectNodeFlowComponent projectNodeFlowComponent;

    @Resource
    private ProductDemandDescFlowComponent productDemandDescFlowComponent;

    public static Map<String, MqMessageHandler> MESSAGE_HANDLER_MAP = new HashMap<>();

    @PostConstruct
    public void init() {
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_TECHREVIEW.getText(), projectFlowComponent::updateFlowInfo);
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_DEMAND_INTERNAL_AUDIT.getText(),projectFlowComponent::updateFlowInfo);
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_DEMAND_CONSTRUE.getText(),projectFlowComponent::updateFlowInfo);
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_UED_AUDIT.getText(),projectFlowComponent::updateFlowInfo);
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_TRACKEVENTREVIEW.getText(), trackEventComponent::updateTrackEventInfo);
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_TRACKEVENTREVIEW_NOTNOTICE.getText(), trackEventComponent::updateTrackEventInfo);
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_PUBLISHOFFICEREVIEW.getText(), projectNodeFlowComponent::updateProjectNodeInfo);
        MESSAGE_HANDLER_MAP.put(MessageTagEnum.FORWARD_PRODUCT_DEMAND_CHANGE.getText(), productDemandDescFlowComponent::updateFlowInfo);

    }

    @Override
    public ReceiveResult receive(List<Msg> list) {
        for (Msg msg : list) {
            String msgId = msg.getMsgId();
            String message = new String(msg.getBody());
            log.info("收到消息, msgId={}, message={}", msgId, message);

            WorkflowBody body = JSON.parseObject(message, WorkflowBody.class);
            try {
                log.info("body: {}", JSON.toJSONString(body));
                MqMessageHandler messageHandler = MESSAGE_HANDLER_MAP.get(body.getProcessDefinitionType());
                if (messageHandler == null) {
                    log.info("找不到消息处理器,message={}", message);
                    return ReceiveResult.success();
                }
                messageHandler.handMessage(body.getProcessInstanceId());
                log.info("消费完成,{}",body.getProcessDefinitionType());
            } catch (Exception e) {
                log.error("消费失败,流程类型={},错误信息={},{}",body.getProcessDefinitionType(),e.getMessage(),e);
            }
        }
        return ReceiveResult.success();
    }
}
