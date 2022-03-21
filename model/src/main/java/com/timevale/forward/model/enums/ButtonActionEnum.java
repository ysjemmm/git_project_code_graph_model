package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @Date 2022/2/25 14:34
 * @Author 望轩
 */
@Getter
public enum ButtonActionEnum {
    /**
     * 确认修复
     */
    CONFIRM_REPAIR("确认修复"),

    /**
     * 自测通过
     */
    SELF_PASS("自测通过"),

    /**
     * 验收通过
     */
    ACCEPTANCE_PASSED("验收通过"),

    /**
     * 验收失败
     */
    ACCEPTANCE_FAILED("验收失败"),

    /**
     * 延期修复
     */
    POSTPONE_REPAIR("延期修复"),

    /**
     * 不用修复
     */
    NO_REPAIR("不用修复"),

    /**
     * 转交
     */
    TRANSMIT("转交"),

    /**
     * 拒绝
     */
    REFUSED("拒绝"),

    /**
     * 同意
     */
    AGREE("同意"),

    /**
     * 重新打开
     */
    OPEN_AGAIN("重新打开"),

    /**
     * 提交
     */
    SUBMIT("提交"),

    /**
     * bug确认
     * */
    CONFIRM("bug确认"),

    /**
     * 开始修复
     * */
    START_REPAIR("开始修复"),

    /**
     * 修复完毕
     * */
    REPAIR_FINISH("修复完毕"),

    /**
     * 已上线
     * */
    ONLINE("已上线"),

    /**
     * 重新确认
     * */
    REPEAT_CONFIRM("重新确认"),

    /**
     * 暂不修复
     * */
    TEMPORARY_NO_REPAIR("暂不修复"),

    /**
     * 修复失败
     * */
    REPAIR_FAIL("修复失败");

    private final String text;

    ButtonActionEnum(String text) {
        this.text = text;
    }
}



























