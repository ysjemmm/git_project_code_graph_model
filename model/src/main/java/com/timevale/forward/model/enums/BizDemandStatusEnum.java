package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @Date 2021/12/15 17:13
 */
@Getter
public enum BizDemandStatusEnum {

    // 需求解决状态:0待评估，10已接收，20已列入项目，30项目进行中，40已完成上线，50被驳回，60已作废

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

    public Integer code;

    BizDemandStatusEnum(int code){this.code = code;}
}
