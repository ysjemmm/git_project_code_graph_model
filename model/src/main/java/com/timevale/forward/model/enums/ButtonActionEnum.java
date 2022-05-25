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
     * 提交
     */
    MODIFY("编辑"),

    /**
     * 创建提测单
     */
    TEST_CREATE("创建提测单"),

    /**
     * 提测通过
     */
    TEST_PASS("提测通过"),

    /**
     * bug确认
     */
    CONFIRM("bug确认"),

    /**
     * 开始修复
     */
    START_REPAIR("开始修复"),

    /**
     * 修复完毕
     */
    REPAIR_FINISH("修复完毕"),

    /**
     * 已上线
     */
    ONLINE("已上线"),

    /**
     * 重新确认
     */
    REPEAT_CONFIRM("重新确认"),

    /**
     * 暂不修复
     */
    TEMPORARY_NO_REPAIR("暂不修复"),

    /**
     * 修复失败
     */
    REPAIR_FAIL("修复失败"),

    /**
     * 转业务需求
     */
    SHIFT_BUSINESS("转业务需求"),

    /**
     * 关联
     */
    LINK("关联"),

    /**
     * 删除关联
     */
    UN_LINK("删除关联"),

    /**
     * 暂停
     */
    SUSPEND("暂停"),
    /**
     * 开启
     */
    ENABLE("开启"),

    /**
     * 作废
     */
    INVALID("作废"),

    /**
     * 接收
     */
    RECEIVE("接收"),
    /**
     * 驳回
     */
    REJECT("驳回"),
    /**
     * 转交
     */
    TRANSFER("转交"),

    /**
     * 发起详设评审
     */
    START_REVIEW("发起详设评审"),

    /**
     * 已处理（无需开发）
     */
    COMPLETED_NOT_DEV("已处理（无需开发）");



    private final String text;

    ButtonActionEnum(String text) {
        this.text = text;
    }
}



























