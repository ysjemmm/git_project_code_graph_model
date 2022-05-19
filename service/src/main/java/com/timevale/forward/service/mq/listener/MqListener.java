package com.timevale.forward.service.mq.listener;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectFlowComponent;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.mq.dto.WorkflowBody;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.framework.mq.client.consumer.Listener;
import com.timevale.framework.mq.client.consumer.ReceiveResult;
import com.timevale.framework.mq.client.producer.Msg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2022/05/17 19:54
 */
@Slf4j
@Component
public class MqListener implements Listener {

    @Resource
    private EpeiusClient epeiusClient;

    @Resource
    MessageEventPublisher messageEventPublisher;

    @Resource
    private ProjectFlowMapper projectFlowMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private ProjectFlowComponent projectFlowComponent;


    @Override
    public ReceiveResult receive(List<Msg> list) {
        for (Msg msg : list) {
            String msgId = msg.getMsgId();
            String message = new String(msg.getBody());
            log.info("收到消息, msgId={}, message={}", msgId, message);

            try {
                WorkflowBody body = JSON.parseObject(message, WorkflowBody.class);
                log.info("body: {}", JSON.toJSONString(body));
//                projectFlowComponent.updateFlowInfo(body.getProcessInstanceId());
                log.info("消费完成");
            } catch (Exception e) {
                log.warn("消费失败", e);
            }
        }
        return ReceiveResult.success();
    }
}
