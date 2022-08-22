package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/08/22 18:19
 */
@Getter
@AllArgsConstructor
public enum OrderCollationEnum {

    ASC(0,"正序"),
    DESC(1,"逆序");

    private Integer code;
    private String text;

}
