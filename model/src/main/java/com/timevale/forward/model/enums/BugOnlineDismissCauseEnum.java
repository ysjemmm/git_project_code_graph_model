package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 14:42
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlineDismissCauseEnum {
    /**
     * 客户操作错误
     */
    OPERATION_MISTAKE(0, "客户操作错误"),

    /**
     * 客户对业务理解错误
     */
    UNDERSTAND_MISTAKE(1, "客户对业务理解错误"),

    /**
     * 产品不支持
     */
    PRODUCT_NO_SUPPORT(2, "产品不支持"),

    /**
     * 客户的回调地址错误
     */
    ADDRESS_MISTAKE(3, "客户的回调地址错误"),

    /**
     * 客户对接版本错误
     */
    VERSION_MISTAKE(4, "客户对接版本错误"),

    /**
     * 配置套餐没有费用
     */
    NO_FREE(5, "配置套餐没有费用"),

    /**
     * 重复提交
     */
    REPEAT_SUBMIT(6, "重复提交"),

    /**
     * 支行大额行号未配置
     */
    NO_CONFIGURE(7, "支行大额行号未配置"),

    /**
     * 实施传参错误
     */
    ARGS_MISTAKE(8, "实施传参错误"),

    /**
     * 网络波动
     */
    NETWORK_FLUCTUATION(9, "网络波动"),

    /**
     * 客户自身缺陷
     */
    CUSTOMER_FAULT(10, "客户自身缺陷"),

    /**
     * 实施给客户项目的配置错误
     */
    PROJECT_CONFIGURE_FAULT(11, "实施给客户项目的配置错误"),

    /**
     * 实施对业务理解错误
     */
    BIZ_UNDERSTAND_FAULT(12, "实施对业务理解错误"),

    /**
     * 操作人录入错误
     */
    OPERATOR_MISTAKE(13, "操作人录入错误"),

    /**
     * 需求变更
     */
    DEMAND_CHANGE(14, "需求变更"),

    /**
     * 历史数据未订正
     */
    DATA_NO_CORRECT(15, "历史数据未订正"),

    /**
     * 文档与实际不符
     */
    DOCUMENT_MISTAKE(16, "文档与实际不符"),

    /**
     * 长时间未反馈
     */
    LONG_TIME_NO_TICKLING(17, "长时间未反馈"),

    /**
     * 问题描述不清
     */
    DES_VAGUE(18, "问题描述不清"),

    /**
     * 当前版本不支持
     */
    CURRENT_VERSION_NONSUPPORT(19, "当前版本不支持"),

    /**
     * 可以升级版本解决
     */
    VERSION_SOLVE(20, "可以升级版本解决"),

    /**
     * 报告人提供信息不全无法排查
     */
    INFORMATION_INCOMPLETE(21, "报告人提供信息不全无法排查"),

    /**
     * 产品配置错误
     */
    PRODUCT_MISTAKE(22, "产品配置错误"),

    /**
     * 客户侧环境问题
     */
    ENV_PROBLEM(23, "客户侧环境问题"),

    /**
     * 技术咨询
     */
    TECHNOLOGY_SUPPORT(24, "技术咨询");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineDismissCauseEnum e : BugOnlineDismissCauseEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "error code";
    }
}
