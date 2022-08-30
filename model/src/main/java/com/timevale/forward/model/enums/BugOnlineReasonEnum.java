package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 15:30
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlineReasonEnum {
    /**
     * 需求问题
     */
    DEMAND_QUESTION(0, "需求问题"),

    /**
     * 环境配置问题
     */
    ENV_CONFIGURE(1, "环境配置问题"),

    /**
     * 功能错误
     */
    FUNCTIONAL_ERROR(2, "功能错误"),

    /**
     * 兼容性问题
     */
    COMPATIBILITY_PROBLEM(3, "兼容性问题"),

    /**
     * 数据问题
     */
    DATA_PROBLEM(4, "数据问题"),

    /**
     * 性能问题
     */
    PERFORMANCE_PROBLEM(5, "性能问题"),

    /**
     * 安全问题
     */
    SAFE_PROBLEM(6, "安全问题"),

    /**
     * 外部原因
     */
    OUTSIDE_REASON(7, "外部原因"),

    /**
     * 开发误操作
     */
    DEVELOP_MISTAKE_OPERATION(8, "开发误操作"),

    /**
     * 接口文档编写错误
     */
    DOCUMENT_MISTAKE(9, "接口文档编写错误"),

    /**
     * 外包项目
     */
    OUT_PROJECT(10, "外包项目"),

    /**
     * 历史版本
     */
    HISTORY_VERSION(11, "历史版本"),

    /**
     * 无测试参与版本
     */
    NO_TEST_VERSION(12, "无测试参与版本"),

    /**
     * 测试环境延期未修复bug
     */
    TEST_ENV_DELAY(13, "测试环境延期未修复bug"),

    /**
     * 设计缺陷
     */
    DEGREE_DEFECT(14, "设计缺陷"),

    /**
     * 定制版升级改动波及
     */
    CHANGE_AFFECT(15, "定制版升级改动波及"),

    /**
     * 无法重现但客户环境偶现
     */
    CUSTOMER_ENV_OCCASION(16, "无法重现但客户环境偶现"),

    /**
     * 无法重现但客户环境必现
     */
    CUSTOMER_ENV_MUST(17, "无法重现但客户环境必现")


    ;

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineReasonEnum e : BugOnlineReasonEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
