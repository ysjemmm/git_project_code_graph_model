package com.timevale.forward.service.mq.listener;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.model.enums.DrcTableEnum;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.handler.DrcHandler;
import com.timevale.forward.service.mq.handler.impl.*;
import com.timevale.framework.mq.client.consumer.Listener;
import com.timevale.framework.mq.client.consumer.ReceiveResult;
import com.timevale.framework.mq.client.producer.Msg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2023/02/10 10:02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrcListener implements Listener {
    private final DrcTaskHandler drcTaskHandler;
    private final DrcProjectHandler drcProjectHandler;
    private final DrcBizDemandHandler drcBizDemandHandler;
    private final DrcBugOnlineHandler drcBugOnlineHandler;
    private final DrcMilestoneActionHandler drcMilestoneActionHandler;
    private final DrcProjectMilestoneHandler drcProjectMilestoneHandler;


    public static Map<DrcTableEnum, DrcHandler> HANDLES = new HashMap<>();

    @PostConstruct
    public void init() {
        HANDLES.put(DrcTableEnum.TASK, drcTaskHandler);
        HANDLES.put(DrcTableEnum.PROJECT, drcProjectHandler);
        HANDLES.put(DrcTableEnum.BIZ_DEMAND, drcBizDemandHandler);
        HANDLES.put(DrcTableEnum.BUG_ONLINE, drcBugOnlineHandler);
        HANDLES.put(DrcTableEnum.PROJECT_MILESTONE, drcProjectMilestoneHandler);
        HANDLES.put(DrcTableEnum.PROJECT_MILESTONE_ACTION, drcMilestoneActionHandler);
    }

    @Override
    public ReceiveResult receive(List<Msg> msgs) {
        for (Msg msg : msgs) {
            String msgId = msg.getMsgId();
            String message = new String(msg.getBody());
            log.info("[DrcListener]收到消息, msgId={}, message={}", msgId, message);

            DrcMsgBody body = JSON.parseObject(message, DrcMsgBody.class);
            try {
                log.info("[DrcListener]body: {}", JSON.toJSONString(body));
                DrcHandler handler = HANDLES.get(DrcTableEnum.getByText(body.getTableName()));
                if (handler == null) {
                    log.error("[DrcListener]找不到处理方法， body: {}", JSON.toJSONString(body));
                } else {
                    handler.handle(body);
                }
                log.info("[DrcListener]消费完成,{}", body.getGtId());
            } catch (Exception e) {
                log.error("[DrcListener]消费失败, topic:{}, msgId:{},e:{}",msg.getTopic(),msgId, e.getMessage());
            }
        }
        return ReceiveResult.success();
    }
}
