package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProductDemandStatusEnum {
    /**
     * 0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废
     */
    WAITING(0),

    INCLUDED(10),

    PROGRESS(20),

    ONLINE(30),

    SUSPEND(40),

    INVALID(50);

    final private Byte code;

    ProductDemandStatusEnum(Integer code) {
        this.code = code.byteValue();
    }
}
