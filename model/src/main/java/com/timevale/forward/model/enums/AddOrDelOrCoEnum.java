package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 添加或del或枚举
 *
 * @author yangxu
 * @date 2023/03/12
 */
@Getter
@AllArgsConstructor
public enum AddOrDelOrCoEnum {

    COEXIST(0,"共存"),

    ADD(1,"新增"),

    DEL(2,"删除");

    private final Integer code;
    private final String text;

}
