package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 10:18
 */
@Getter
@AllArgsConstructor
public enum BizProductLineTypeEnum {
    // 线上bug
    BUG_ONLINE(0),

    // 故障提单
    TROUBLE_TICKET(1),

    ;

    private final Integer code;

}
