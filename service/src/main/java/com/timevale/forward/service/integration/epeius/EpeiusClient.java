package com.timevale.forward.service.integration.epeius;

import com.timevale.epeius.service.model.request.ProcessInstanceRequest;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.epeius.service.model.request.TerminateRequest;
import com.timevale.epeius.service.model.response.FlowResponse;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.lowcode.support.response.task.TaskHandleUserResponse;

/**
 * @author xingyun
 * @date 2022/5/12 10:57
 */
public interface EpeiusClient {

    /**
     * 发起工作流
     * @param start 发起参数
     * @return 流程实例id
     */
    String start(StartProcessRequest start);

    /**
     * 流程
     * @param processInstanceId processInstanceId
     * @return 流程信息
     */
    ProcessResponse getProcessInfo(String processInstanceId);

    /**
     * 流程
     * @param processInstanceId processInstanceId
     * @return 流程信息
     */
    FlowResponse getOldProcessInfo(String processInstanceId);

    /**
     *
     * @param taskId taskId
     * @return 人员信息
     */
    TaskHandleUserResponse getTaskHandleUserList(String taskId);


    /**
     * 撤回审批流
     * @param terminateRequest terminateRequest
     * @return Boolean
     */
    Boolean withdrawInstance(TerminateRequest terminateRequest);


    Boolean addVariables(ProcessInstanceRequest processInstanceRequest);

}
