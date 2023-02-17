package com.timevale.forward.service.observer.event;

import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;
import java.util.Objects;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
public class TaskDoneMsgEvent extends MessageEvent {

    private final String operator;
    private final List<String> receivers;
    private final String taskName;
    private final Long taskId;
    private final ProjectDO project;

    private static final String TASK_DONE_MSG = "### %s  \n  **%s**已完成: **%s**  \n\n  ***  \n  [查看详情](%s)";

    public TaskDoneMsgEvent(Object source, String operator, List<String> receivers, String taskName, Long taskId, ProjectDO project) {
        super(source);
        this.operator = operator;
        this.receivers = receivers;
        this.taskName = taskName;
        this.taskId = taskId;
        this.project = project;
    }

    @Override
    public void run() {
        String param = "%s/edit?id=%d&projectId=%d&type=check";
        String title = MessageTitleEnum.TASK_DONE.getText();
        String singleUrl = domainName + String.format(param,
                Objects.equals(project.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode()) ?
                        TabEnum.INNER_TASK_MANAGEMENT.getText() : TabEnum.TASK_MANAGEMENT.getText(),
                taskId, project.getId());
        String markdown = String.format(TASK_DONE_MSG, title, operator, taskName, singleUrl);
        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();
        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}