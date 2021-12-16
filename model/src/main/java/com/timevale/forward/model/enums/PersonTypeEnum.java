package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @Date 2021/12/16 09:50
 */
@Getter
public enum PersonTypeEnum {
    // 0 项目-产品经理
    PROJECT_PD(0),
    // 1 项目-项目成员
    PROJECT_MEMBER(1),
    // 20 产品需求-抄送人
    PRODUCT_DEMAND_CC(20),
    // 30 业务需求-抄送人
    BIZ_DEMAND_CC(30);

    private Byte code;

    PersonTypeEnum(Integer code){this.code = code.byteValue();}

}
