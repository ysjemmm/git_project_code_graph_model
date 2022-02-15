package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @Date 2022/1/29 11:01
 * @Author 望轩
 */
@Getter
public enum TestBillProgressEnum {
    /**
     * 用例执行情况:0冒烟用例执行通过,1冒烟用例部分执行,2冒烟用例未执行
     */

    SMOKING_PASS(0, "冒烟用例执行通过"),

    SMOKING_PART_EXECUTE(1, "冒烟用例部分执行"),

    SMOKING_NO_EXECUTE(2, "冒烟用例未执行");

    final private String text;
    final private Integer code;

    TestBillProgressEnum(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code) {
        for (TestBillProgressEnum e : TestBillProgressEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }


}
