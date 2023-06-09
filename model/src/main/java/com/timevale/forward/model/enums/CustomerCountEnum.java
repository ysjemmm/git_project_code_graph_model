package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/06/09 17:38
 */
@Getter
@AllArgsConstructor
public enum CustomerCountEnum {
    NONE(0,"无"),
    ONE(1,"单客户"),
    GEQ_TWO(2,"2家或2家以上客户");

    private final Integer code;
    private final String text;
}
