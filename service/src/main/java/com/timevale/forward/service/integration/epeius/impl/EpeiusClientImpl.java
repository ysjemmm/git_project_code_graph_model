package com.timevale.forward.service.integration.epeius.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.epeius.service.api.FlowService;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.base.PageResult;
import com.timevale.epeius.service.model.request.ProcessInstanceRequest;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.epeius.service.model.request.TerminateRequest;
import com.timevale.epeius.service.model.response.FlowResponse;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.lowcode.support.api.ProcessQueryRpcService;
import com.timevale.lowcode.support.api.TaskQueryRpcService;
import com.timevale.lowcode.support.request.process.ProcessQueryRequest;
import com.timevale.lowcode.support.request.task.TaskHandleUserQueryRequest;
import com.timevale.lowcode.support.response.RpcResponse;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.lowcode.support.response.task.TaskHandleUserResponse;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author xingyun
 * @date 2022/5/12 10:57
 */
@Slf4j
@Component
public class EpeiusClientImpl implements EpeiusClient {

    @Resource
    private FlowService flowService;

    @Resource
    private ProcessQueryRpcService processQueryRpcService;

    @Resource
    private TaskQueryRpcService taskQueryRpcService;

    @Override
    public String start(StartProcessRequest start) {
        try {
            log.info("发起工作流 start: {}", JSON.toJSONString(start));
            BaseResult<String> result = flowService.start(start);
            if (result == null || !result.ifSuccess() || result.getData() == null) {
                log.error("发起工作流失败 result: {}", result);
                throw new BaseBizRuntimeException("发起工作流失败");
            }
            log.info("发起工作流 result: {}", result.getData());
            return result.getData();
        } catch (Exception e) {
            log.warn("发起工作流异常: ", e);
            throw new BaseBizRuntimeException("发起工作流异常");
        }
    }

    @Override
    public ProcessResponse getProcessInfo(String processInstanceId) {
        try {
            log.info("查询工作流 processInstanceId: {}", processInstanceId);
            ProcessQueryRequest request=new ProcessQueryRequest();
            request.setProcessInstanceId(processInstanceId);
            RpcResponse<ProcessResponse> response = processQueryRpcService.getProcessInfo(request);
            if (response == null || response.getData()==null) {
                log.error("查询工作流 response: {}", response);
                throw new BaseBizRuntimeException("查询工作流失败");
            }
            log.info("查询工作流 response: {}", response.getData());
            return response.getData();
        } catch (Exception e) {
            log.warn("查询工作流失败: ", e);
            throw new BaseBizRuntimeException("查询工作流失败");
        }
    }

    @Override
    public FlowResponse getOldProcessInfo(String processInstanceId) {
        try {
            log.info("查询工作流 processInstanceId: {}", processInstanceId);

            BaseResult<FlowResponse> result = flowService.getOneProcessInstance(processInstanceId);
            if (!result.ifSuccess() || Objects.isNull(result.getData())) {
                log.error("查询工作流 response: {}", result);
                throw new BaseBizRuntimeException("查询工作流失败");
            }

            log.info("查询工作流 response: {}", result);
            return result.getData();
        } catch (Exception e) {
            log.warn("查询工作流失败: ", e);
            throw new BaseBizRuntimeException("查询工作流失败");
        }
    }

    @Override
    public TaskHandleUserResponse getTaskHandleUserList(String processInstanceId) {
        try {
            log.info("查询工作流人员信息 processInstanceId: {}", processInstanceId);
            TaskHandleUserQueryRequest request=new TaskHandleUserQueryRequest();
            request.setTaskId(processInstanceId);
            RpcResponse<TaskHandleUserResponse> response = taskQueryRpcService.getTaskHandleUserList(request);
            if (response == null || response.getData()==null) {
                log.error("查询工作流人员信息 response: {}", response);
                throw new BaseBizRuntimeException("查询工作流人员信息失败");
            }
            log.info("查询工作流人员信息 result: {}", response.getData());
            return response.getData();
        } catch (Exception e) {
            log.warn("查询工作流人员信息失败: ", e);
            throw new BaseBizRuntimeException("查询工作流人员信息失败");
        }
    }

    @Override
    public Boolean withdrawInstance(TerminateRequest terminateRequest) {
        try {
            ProcessResponse processInfo = getProcessInfo(terminateRequest.getProcessInstanceId());
            if(!FlowStatusEnum.PENDING.getValue().equals(processInfo.getProcessStatus())){
                return true;
            }
            log.info("撤回工作流 withdrawInstance: {}", JSON.toJSONString(terminateRequest));
            BaseResult<Boolean> result = flowService.withdrawInstance(terminateRequest);
            if (result == null || !result.ifSuccess() || result.getData() == null) {
                log.error("撤回工作流失败 result: {}", result);
                throw new BaseBizRuntimeException("撤回工作流失败");
            }
            log.info("撤回工作流 result: {}", result.getData());
            return result.getData();
        } catch (Exception e) {
            log.warn("撤回工作流异常: ", e);
            throw new BaseBizRuntimeException("撤回工作流异常");
        }
    }

    @Override
    public Boolean addVariables(ProcessInstanceRequest processInstanceRequest) {
        try {
            log.info("添加流程变量 withdrawInstance: {}", JSON.toJSONString(processInstanceRequest));
            BaseResult<Boolean> result = flowService.addVariables(processInstanceRequest);
            if (result == null || !result.ifSuccess() || result.getData() == null) {
                log.error("添加流程变量 result: {}", result);
                throw new BaseBizRuntimeException("添加流程变量");
            }
            log.info("添加流程变量 result: {}", result.getData());
            return result.getData();
        } catch (Exception e) {
            log.warn("添加流程变量: ", e);
            throw new BaseBizRuntimeException("添加流程变量");
        }
    }

}
