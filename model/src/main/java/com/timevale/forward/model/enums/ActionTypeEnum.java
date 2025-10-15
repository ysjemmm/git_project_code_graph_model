package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @auther: yuhua
 * @date: 2025/10/10 14:45
 * @description:
 */
@Getter
public enum ActionTypeEnum {
    /**
     * send_notification:发送通知
     */
    SEND_NOTIFICATION("send_notification"),

    /**
     * execute_task:执行任务
     */
    EXECUTE_TASK("execute_task");

    private final String code;
    ActionTypeEnum(String code){
        this.code = code;
    }
}
