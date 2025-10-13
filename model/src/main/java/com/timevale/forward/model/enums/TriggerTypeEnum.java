package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @auther: yuhua
 * @date: 2025/10/10 14:45
 * @description:
 */
@Getter
public enum TriggerTypeEnum {
    /**
     * scheduled:定时触发
     */
    SCHEDULED("scheduled"),

    /**
     * status_change:状态变更
     */
    STATUS_CHANGE("status_change");

    private final String code;
    TriggerTypeEnum(String code){
        this.code = code;
    }
}
