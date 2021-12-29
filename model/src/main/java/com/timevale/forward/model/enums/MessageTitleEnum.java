package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 18:12
 */
@Getter
public enum MessageTitleEnum {
    // 业务需求状态变更通知标题
    BIZDEMAND_FEEDBACK("业务需求反馈通知"),
    BIZDEMAND_STATUS_CHANGE("业务需求进度变更通知"),
    BIZDEMAND_RECEIVE("业务需求接收通知"),
    BIZDEMAND_INVALID("业务需求作废通知"),
    // 评论区@通知标题
    COMMENT("评论区");

    private String text;

    MessageTitleEnum(String text){this.text = text;}
}
