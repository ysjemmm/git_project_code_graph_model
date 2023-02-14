package com.timevale.forward.model.event;

import com.timevale.forward.dal.entity.ProjectDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author jingchun
 * created on 2023/2/14
 */
@Getter
public class ProjectCreateEvent extends ApplicationEvent {

    private final ProjectDO project;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public ProjectCreateEvent(Object source, ProjectDO project) {
        super(source);
        this.project = project;
    }
}
