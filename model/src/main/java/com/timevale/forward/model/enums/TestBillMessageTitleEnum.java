package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @Date 2022/2/10 18:09
 * @Author 望轩
 */
@Getter
public enum TestBillMessageTitleEnum {
    SUBMIT_SMOKING_TEST("提交测试用例通知"),
    TEST_MAN_UPDATE("测试人更新通知"),
    SUBMIT_TEST_SHOW("提测预演通知"),
    SELF_TEST("自测通知"),
    SUBMIT_TEST_FAIL("提测失败通知"),
    SUBMIT_TEST_SUCCESS("提测成功通知"),
    ;


    private final String text;

    TestBillMessageTitleEnum(String text) {
        this.text = text;
    }
}
