package com.timevale.forward.service.observer.event;

import com.timevale.forward.dal.entity.BugOnlineDO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class OnlineBugStatusChangeEvent extends ApplicationEvent {

    private final BugOnlineDO bugOnline;
    private final String oldStatus;
    private final String newStatus;
    private final String operator;

    public OnlineBugStatusChangeEvent(Object source, BugOnlineDO bugOnline, String oldStatus, String newStatus, String operator) {
        super(source);
        this.bugOnline = bugOnline;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.operator = operator;
    }
}