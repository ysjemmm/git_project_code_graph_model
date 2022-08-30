package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Collections;

public class WorkflowRejectMsgEvent extends MessageEvent {

    private final String flowName;

    private final String createManId;

    private final String taskId;

    private final String createMan;

    private final String createDate;

    private final String rejectReason;

    public WorkflowRejectMsgEvent(Object source, String flowName, String createManId, String taskId, String createMan,
                                  String createDate, String rejectReason) {
        super(source);
        this.flowName = flowName;
        this.createManId = createManId;
        this.taskId = taskId;
        this.createMan = createMan;
        this.createDate = createDate;
        this.rejectReason = rejectReason;
    }

    @Override
    public void run() {
       String singleUrl= config.getWorkflowBaseUrl() + taskId;
        MarkdownMsg actionCard = MarkdownMsg.builder().title("审批拒绝通知")
                .receivers(Collections.singletonList(createManId))
                .content(String.format(
                        "### 你提交的%s流程已驳回，请知晓\n\n" +
                                "发起人: **%s**\n\n" +
                                "发起时间: **%s**\n\n" +
                                "审批原因: **%s**  \n  [查看详情](%s)",
                        flowName,
                        createMan, createDate, rejectReason,singleUrl))
                .build();
        erpMessageClient.sendMarkdownMsg(actionCard);
    }
}
