package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.DingRelationMapper;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.DingRelationDO;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.DingRelationTypeEnum;
import com.timevale.forward.model.enums.DingTypeEnum;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.AddDingTodoReq;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.handler.DrcHandler;
import com.timevale.forward.service.observer.event.BugOnlineTemporarySolutionMsg;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
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

    private final DingRelationMapper dingRelationMapper;

    /**
     * 线上BUG地址
     */
    @Value("${forward.baseurl}/mainBugManagement/edit?id=%s&type=check")
    private String bugOnlineUrl;

    @Override
    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(() -> msgHandle(body));
    }

    private void msgHandle(DrcMsgBody drcMsgBody) {
        BugOnlineDO beforeBug = JSON.parseObject(drcMsgBody.getBefore(), BugOnlineDO.class);
        BugOnlineDO afterBug = JSON.parseObject(drcMsgBody.getAfter(), BugOnlineDO.class);
        if (!Objects.equals(beforeBug.getStatus(), afterBug.getStatus())) {
            if (Objects.equals(afterBug.getStatus(), BugOnlineStatusEnum.BE_CONFIRM.getCode()) ||
                    Objects.equals(afterBug.getStatus(), BugOnlineStatusEnum.ACCEPTANCE.getCode())) {
                createTodo(afterBug);
            } else if (Objects.equals(afterBug.getStatus(), BugOnlineStatusEnum.CLOSE.getCode()) ||
                    Objects.equals(afterBug.getStatus(), BugOnlineStatusEnum.COMPLETE.getCode())) {
                finishTodo(afterBug);
            }
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
        String dingTaskId = dingWorkRecordClient.addTodoTask(new AddDingTodoReq()
                .title("产研系统线上bug待验收")
                .url(String.format(bugOnlineUrl, bugOnlineDO.getId()))
                .creatorId(bugOnlineDO.getProposerId())
                .receiveId(bugOnlineDO.getProposerId())
                .content(bugOnlineDO.getName() + "需要尽快完成验收并反馈客户，请确认")
                .dueTime(LocalDateTime.now().plusHours(1L).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        if (StringUtils.isNotEmpty(dingTaskId)) {
            dingRelationMapper.insert(new DingRelationDO()
                    .setDingType(DingTypeEnum.TODO_TASK.ordinal())
                    .setRelationType(DingRelationTypeEnum.BUG_ONLINE.ordinal())
                    .setDingId(dingTaskId)
                    .setRelationId(bugOnlineDO.getId()));
        }
    }

    private void finishTodo(BugOnlineDO bugOnlineDO) {
        List<DingRelationDO> relations = dingRelationMapper.getByRelation(DingRelationTypeEnum.BUG_ONLINE.ordinal(),
                bugOnlineDO.getId(), DingTypeEnum.TODO_TASK.ordinal());
        relations.stream().max(Comparator.comparing(DingRelationDO::getCreateDate)).ifPresent(relation ->
                dingWorkRecordClient.finishTodoTask(relation.getDingId(), bugOnlineDO.getProposerId()));
    }

}
