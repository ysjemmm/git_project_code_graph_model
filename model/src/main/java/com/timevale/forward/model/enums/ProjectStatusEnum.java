package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProjectStatusEnum {
    /**
     * 0待启动,10规划中,20研发中,30测试中,40已发布,50已暂停,60已作废
     */
    WAITING(0),

    PLANING(10),

    DEVING(20),

    TESTING(30),

    RELEASED(40),

    SUSPEND(50),

    INVALID(60);

    final private Byte code;

    ProjectStatusEnum(Integer code) {
        this.code = code.byteValue();
    }
}
