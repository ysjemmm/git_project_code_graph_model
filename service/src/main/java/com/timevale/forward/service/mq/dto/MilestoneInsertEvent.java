package com.timevale.forward.service.mq.dto;

import com.timevale.forward.dal.entity.ProjectMilestone;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Getter
public class MilestoneInsertEvent extends ApplicationEvent {

    private final ProjectMilestone data;

    public MilestoneInsertEvent(Object source, ProjectMilestone data) {
        super(source);
        this.data = data;
    }
}
