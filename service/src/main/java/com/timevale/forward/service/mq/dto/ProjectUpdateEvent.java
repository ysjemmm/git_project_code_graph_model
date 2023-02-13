package com.timevale.forward.service.mq.dto;

import com.timevale.forward.dal.entity.ProjectDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Getter
public class ProjectUpdateEvent extends ApplicationEvent {

    private final ProjectDO data;

    public ProjectUpdateEvent(Object source, ProjectDO data) {
        super(source);
        this.data = data;
    }
}
