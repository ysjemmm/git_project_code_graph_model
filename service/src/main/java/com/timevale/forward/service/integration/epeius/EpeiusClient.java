package com.timevale.forward.service.integration.epeius;

import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.epeius.service.model.response.ProcessLogResponse;

import java.util.List;

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
     * 流程日志
     * @param processInstanceId processInstanceId
     * @return 流程日志信息
     */
    List<ProcessLogResponse> flowLog(String processInstanceId);
}
