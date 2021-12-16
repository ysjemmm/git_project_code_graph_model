package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @Date 2021/12/16 10:18
 */
@Getter
public enum FileTypeEnum {
    // 产品需求
    PRODUCT_DEMAND(1),
    // 业务需求
    BIZ_DEMAND(2);

    private Byte code;

    FileTypeEnum(Integer code){this.code = code.byteValue();}
}
