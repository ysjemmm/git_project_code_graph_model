package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.AddDingTodoReq;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.handler.DrcHandler;
import com.timevale.forward.service.observer.event.BugOnlineTemporarySolutionMsg;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Objects;

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

    private final DingWorkRecordClient dingWorkRecordClient;

    @Override
    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(() -> msgHandle(body));
    }

    private void msgHandle(DrcMsgBody drcMsgBody) {
        BugOnlineDO beforeBug = JSON.parseObject(drcMsgBody.getBefore(), BugOnlineDO.class);
        BugOnlineDO afterBug = JSON.parseObject(drcMsgBody.getAfter(), BugOnlineDO.class);
        if (Objects.equals(afterBug.getStatus(), BugOnlineStatusEnum.BE_CONFIRM.getCode()) ||
                Objects.equals(beforeBug.getStatus(), BugOnlineStatusEnum.ACCEPTANCE.getCode())) {
            createTodo(afterBug);
        }

        if (ObjectUtil.notEqual(beforeBug.getTemporarySolution(), afterBug.getTemporarySolution())
                && StrUtil.isNotEmpty(afterBug.getTemporarySolution())) {
            new BugOnlineTemporarySolutionMsg(
                    this,
                    afterBug.getName(),
                    afterBug.getProposerId(),
                    afterBug.getId()
            ).send();
        }
    }

    private void createTodo(BugOnlineDO bugOnlineDO) {
        // TODO jingchun 完成待办任务参数填写
        dingWorkRecordClient.addTask(new AddDingTodoReq());
    }


}
