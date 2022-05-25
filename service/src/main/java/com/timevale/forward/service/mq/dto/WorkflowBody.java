package com.timevale.forward.service.mq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @author xingyun
 * @date 2022/05/17 19:54
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowBody {

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 消息
     */
    private String message;


    /**
     * 工作流处理结果类型
     */
    private String status;

    /**
     * 工作流类型
     */
    private String processDefinitionType;
}
