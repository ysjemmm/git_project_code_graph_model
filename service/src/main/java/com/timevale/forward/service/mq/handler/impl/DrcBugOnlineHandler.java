package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.handler.DrcHandler;
import com.timevale.forward.service.observer.event.BugOnlineTemporarySolutionMsg;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2023/06/14 17:57
 */
@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class DrcBugOnlineHandler implements DrcHandler {
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(()-> msgHandle(body));
    }

    private void msgHandle(DrcMsgBody drcMsgBody) {
        BugOnlineDO beforeBug = JSON.parseObject(drcMsgBody.getBefore(), BugOnlineDO.class);
        BugOnlineDO afterBug = JSON.parseObject(drcMsgBody.getAfter(), BugOnlineDO.class);

        if (ObjectUtil.notEqual(beforeBug.getTemporarySolution(), afterBug.getTemporarySolution())) {
            new BugOnlineTemporarySolutionMsg(
                    this,
                    afterBug.getName(),
                    afterBug.getProposerId(),
                    afterBug.getId()
            ).send();
        }
    }
}
