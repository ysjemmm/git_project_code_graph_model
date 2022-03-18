package com.timevale.forward.service.integration.erp.impl;

import com.timevale.erp.message.service.api.DingWorkRecordService;
import com.timevale.erp.message.service.model.DingCreateTodoTaskInput;
import com.timevale.erp.message.service.model.DingDeleteTodoTaskInput;
import com.timevale.erp.message.service.model.DingGetTodoTaskInput;
import com.timevale.erp.message.service.model.DingUpdateTodoTaskInput;
import com.timevale.erp.message.service.result.DingTodoTaskResponseBody;
import com.timevale.forward.service.integration.erp.DingWorkRecordClient;
import com.timevale.forward.service.integration.erp.model.CreateTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.DeleteTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.GetTodoTaskMsg;
import com.timevale.forward.service.integration.erp.model.UpdateTodoTaskMsg;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.result.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
    public String addTask(CreateTodoTaskMsg createTodoTaskMsg) {
        final DingCreateTodoTaskInput input = new DingCreateTodoTaskInput();
        input.setTitle(createTodoTaskMsg.getTitle());
        input.setUnionId(createTodoTaskMsg.getUnionId());
        input.setExecutorIds(createTodoTaskMsg.getExecutorIds());
        input.setDueTime(createTodoTaskMsg.getDueTime());
        try {
            return dingWorkRecordService.addTask(input).getResultObject();
        }catch (Exception e){
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
        }catch (Exception e){
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
        }catch (Exception e){
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
            if(task.isSuccess()){
                return dingWorkRecordService.getTask(input).getResultObject();
            }
        }catch (Exception e){
            log.error("[erpMessage]获取待办失败  error: " + e.getMessage() + " 发送通知信息：" + getTodoTaskMsg);
        }
        throw new BaseBizRuntimeException("获取待办信息失败");
    }
}
