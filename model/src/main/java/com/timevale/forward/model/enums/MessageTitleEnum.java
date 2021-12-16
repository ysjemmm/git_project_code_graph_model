package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 18:12
 */
@Getter
public enum MessageTitleEnum {
    // 业务需求状态变更通知标题
    BIZDEMAND("业务需求反馈通知"),
    // 评论区@通知标题
    COMMENT("有人@你啦");

    private String text;

    MessageTitleEnum(String text){this.text = text;}
}
