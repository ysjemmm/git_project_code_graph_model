package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @Date 2022/1/29 11:48
 * @Author 望轩
 */
@Getter
public enum TestBillStatusEnum {
    /**
     * 项目状态:-1提前提交测试用例,0待提交冒烟用例1待自测,2待提测预演,3提测成功
     */
    PRE_SUBMIT_TEST_CASE(-1, "提前提交测试用例"),
    NO_SUBMIT_SMOKING(0, "待提交测试用例"),
    NO_SELF_TEST(1, "待自测"),
    NO_TEST_PREVIEW(2, "待提测预演"),
    TEST_SUCCESS(3, "提测成功");

    private final Integer code;
    private final String text;

    TestBillStatusEnum(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code) {
        for (TestBillStatusEnum e : TestBillStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }
}
