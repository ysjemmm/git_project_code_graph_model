package com.timevale.forward.service.mq.dto;

import com.timevale.forward.dal.entity.TaskDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Getter
public class TaskUpdateEvent extends ApplicationEvent {

    private final TaskDO data;

    public TaskUpdateEvent(Object source, TaskDO data) {
        super(source);
        this.data = data;
    }

}
