package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @auther: yuhua
 * @date: 2025/10/10 14:45
 * @description:
 */
@Getter
public enum ReceiverTypeEnum {
    /**
     * operator:经办人
     */
    OPERATOR("operator"),

    /**
     * custom_users:自定义接收人
     */
    CUSTOM_USERS("custom_users");

    private final String code;
    ReceiverTypeEnum(String code){
        this.code = code;
    }
}
