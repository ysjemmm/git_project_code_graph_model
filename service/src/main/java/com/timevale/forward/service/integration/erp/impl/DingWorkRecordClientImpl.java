package com.timevale.forward.service.integration.erp.impl;

import com.timevale.erp.message.service.api.DingWorkRecordService;
import com.timevale.erp.message.service.model.*;
import com.timevale.erp.message.service.result.DingTodoTaskResponseBody;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.*;
import com.timevale.mandarin.common.result.BaseResult;
import com.timevale.mandarin.common.result.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 钉钉待办消息
 *
 * @author xingyun
 * @date 2022/01/25 10:51
 */
@Slf4j
@Service
public class DingWorkRecordClientImpl implements DingWorkRecordClient {

    @Resource
    private DingWorkRecordService dingWorkRecordService;

    @Override
    public String addTask(AddDingTodoReq req) {

        String title = req.title();
        String receiveId = req.receiveId();
        String url = req.url();

        if (StringUtils.isEmpty(title)
                || StringUtils.isEmpty(receiveId)
                || StringUtils.isEmpty(url)
                || StringUtils.isEmpty(req.content())) {
            log.warn("[ErpMsgCaller.createDingWorkRecord] 参数有误: title={}, receiveId={}, url={}", title, receiveId, url);
            return null;
        }

        DingWorkRecordContent content = new DingWorkRecordContent();
        content.setTitle(req.title());
        content.setContent(req.content());

        DingWorkRecordAddInput input = new DingWorkRecordAddInput();

        input.setTitle(title);
        input.setReceive(receiveId);
        input.setUrl(url);
        input.setContents(Collections.singletonList(content));
        input.setPcOpenType(2);

        try {
            log.info("[ErpMsgCaller.createDingWorkRecord] request req: {}", input);
            QueryResult<String> result = dingWorkRecordService.add(input);
            log.info("[ErpMsgCaller.createDingWorkRecord] result: {}", result);
            if (result != null && result.isSuccess()) {
                return result.getResultObject();
            }
        } catch (Throwable e) {
            log.error("[ErpMsgCaller.createDingWorkRecord] 创建钉钉待办异常: title=" + title, e);
        }

        return null;
    }

    @Override
    public void finishTask(String recordId, String userId) {
        DingWorkRecordUpdateInput input = new DingWorkRecordUpdateInput();
        input.setRecordId(recordId);
        input.setUserId(userId);
        try {
            log.info("[ErpMsgCaller.finishDingWorkRecord] request param: {}", input);
            BaseResult result = dingWorkRecordService.finishWorkRecord(input);
            log.info("[ErpMsgCaller.finishDingWorkRecord] result: {}", result);
        } catch (Throwable e) {
            log.error("[ErpMsgCaller.finishDingWorkRecord] 更新钉钉待办异常: recordId={}", recordId, e);
        }
    }

    @Override
    public String addTask(CreateTodoTaskMsg createTodoTaskMsg) {
        final DingCreateTodoTaskInput input = new DingCreateTodoTaskInput();
        input.setTitle(createTodoTaskMsg.getTitle());
        input.setUnionId(createTodoTaskMsg.getUnionId());
        input.setExecutorIds(createTodoTaskMsg.getExecutorIds());
        input.setDueTime(createTodoTaskMsg.getDueTime());
        try {
            return dingWorkRecordService.addTask(input).getResultObject();
        } catch (Exception e) {
            log.error("[erpMessage]创建待办失败  error: " + e.getMessage() + " 发送通知信息：" + createTodoTaskMsg);
        }
        return StringUtils.EMPTY;
    }

    @Override
    public void updateTask(UpdateTodoTaskMsg updateTodoTaskMsg) {
        final DingUpdateTodoTaskInput input = new DingUpdateTodoTaskInput();
        input.setRecordId(updateTodoTaskMsg.getRecordId());
        input.setTitle(updateTodoTaskMsg.getTitle());
        input.setUnionId(updateTodoTaskMsg.getUnionId());
        input.setExecutorIds(updateTodoTaskMsg.getExecutorIds());
        input.setParticipantIds(updateTodoTaskMsg.getParticipantIds());
        input.setDone(updateTodoTaskMsg.getDone());
        input.setDueTime(updateTodoTaskMsg.getDueTime());
        try {
            dingWorkRecordService.updateTask(input);
        } catch (Exception e) {
            log.error("[erpMessage]更新待办失败  error: " + e.getMessage() + " 发送通知信息：" + updateTodoTaskMsg);
        }
    }

    @Override
    public void deleteTask(DeleteTodoTaskMsg deleteTodoTaskMsg) {
        final DingDeleteTodoTaskInput input = new DingDeleteTodoTaskInput();
        input.setRecordId(deleteTodoTaskMsg.getRecordId());
        input.setUnionId(deleteTodoTaskMsg.getUnionId());
        try {
            dingWorkRecordService.deleteTask(input);
        } catch (Exception e) {
            log.error("[erpMessage]删除待办失败  error: " + e.getMessage() + " 发送通知信息：" + deleteTodoTaskMsg);
        }
    }

    @Override
    public DingTodoTaskResponseBody getTask(GetTodoTaskMsg getTodoTaskMsg) {
        final DingGetTodoTaskInput input = new DingGetTodoTaskInput();
        input.setRecordId(getTodoTaskMsg.getRecordId());
        input.setUnionId(getTodoTaskMsg.getUnionId());
        try {
            QueryResult<DingTodoTaskResponseBody> task = dingWorkRecordService.getTask(input);
            if (task.isSuccess()) {
                return dingWorkRecordService.getTask(input).getResultObject();
            }
        } catch (Exception e) {
            log.error("[erpMessage]获取待办失败  error: " + e.getMessage() + " 发送通知信息：" + getTodoTaskMsg);
        }
        return null;
    }

    @Override
    public Map<String, DingTodoTaskResponseBody> batchGetTask(List<GetTodoTaskMsg> getTodoTaskMsgList) {
        Map<String, DingTodoTaskResponseBody> result = new ConcurrentHashMap<>();
        getTodoTaskMsgList.parallelStream()
                .forEach(e -> {
                    DingTodoTaskResponseBody task = getTask(e);
                    if (task != null) {
                        result.put(task.getId(), task);
                    }
                });
        return result;
    }
}
