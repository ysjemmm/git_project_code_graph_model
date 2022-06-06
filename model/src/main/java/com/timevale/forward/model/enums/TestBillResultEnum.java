package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2022/05/25 17:33
 */
@Getter
@AllArgsConstructor
public enum TestBillResultEnum {

    NO_START(0, "未提测"),
    TESTING(1, "提测中"),
    SUCCESS(2, "提测成功"),
    FAIL(3, "提测失败");

    private Integer code;
    private String text;
}
