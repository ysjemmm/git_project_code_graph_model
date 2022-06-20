package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 @Getter
 */
@Getter
public enum MessageTagEnum {
    /**
     * 项目流程状态
     */
    FORWARD_TECHREVIEW("forward_techReview"),

    FORWARD_TRACKEVENTREVIEW("forward_trackEventReview");

    private final String text;

    MessageTagEnum(String text){
        this.text = text;
    }

}
