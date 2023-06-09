package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/06/09 17:40
 */
@Getter
@AllArgsConstructor
public enum UserCountEnum {

    LEQ_TWO(1,"1~2个"),
    GEQ_THI(2,"3个或3个以上");

    private final Integer code;
    private final String text;
}
