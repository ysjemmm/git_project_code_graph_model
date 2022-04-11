package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizDemandActionEnum {

    SUBMIT("提交"),

    INVALID("作废"),

    RECEIVE("接收"),

    LINK("关联"),

    UNLINK("删除关联"),

    REJECT("驳回"),

    TRANSFER("转交");

    private final String text;

}
