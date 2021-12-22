package com.timevale.forward.model.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 */
@Getter
public enum BizDemandStatusEnum {
    // 待评估
    EVALUATE(0),
    // 已接收
    RECEIVED(10),
    // 已列入项目
    INCLUDE_PROJECT(20),
    // 项目进行中
    PROJECTING(30),
    // 已经完成上线
    AVAILABLE(40),
    // 被驳回
    REJECT(50),
    // 已作废
    INVALID(60);

    private Integer code;

    BizDemandStatusEnum(Integer code){this.code = code;}
}
