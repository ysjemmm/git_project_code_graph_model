package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/03/01 13:42
 */
@Getter
@AllArgsConstructor
public enum BugReasonEnum {
    /**
     * 功能错误
     */
    FUNCTION_ERROR(0,"功能错误"),
    /**
     * 功能缺失
     */
    FUNCTION_LACK(1,"功能缺失"),
    /**
     * 改动波及
     */
    CHANGE_AFFECT(2,"改动波及"),
    /**
     * 参数校验错误
     */
    PARAMETER_VALID(3,"参数校验错误"),
    /**
     * 历史遗留
     */
    HISTORY_LEFT(4,"历史遗留"),
    /**
     * 实现与需求不符
     */
    FUNCTION_DEMAND_NOT_CONFORM(5,"实现与需求不符"),
    /**
     * 配置错误
     */
    CONFIG_ERROR(6,"配置错误"),
    /**
     * 环境部署
     */
    ENV_DEPLOY(7,"环境部署"),
    /**
     * 页面格式错误
     */
    PAGE_FORMAT_ERROR(8,"页面格式错误"),
    /**
     * 文案提示
     */
    TIP(9,"文案提示"),
    /**
     * UI和原型不一致
     */
    UI_PROTOTYPE_NOT_CONFORM(10,"UI和原型不一致"),
    /**
     * 数据问题
     */
    DATE(11,"数据问题"),
    /**
     * 需求问题
     */
    DEMAND(12,"需求问题"),
    /**
     * 兼容性问题
     */
    COMPATIBILITY(13,"兼容性问题"),
    /**
     * 交互体验
     */
    INTERACTION_EXPERIENCE(14,"交互体验"),
    /**
     * 交付文档错误
     */
    DOCUMENT_ERROR(15,"交付文档错误"),
    /**
     * 优化建议
     */
    OPTIMIZE_ADVICE(16,"优化建议"),
    /**
     * 性能问题
     */
    PERFORMANCE(17,"性能问题"),
    /**
     * 安全问题
     */
    SAFE(18,"安全问题"),
    /**
     * 数据库问题
     */
    DATABASE(19,"数据库问题"),
    /**
     * 低级错误
     */
    LOW_LEVEL(20,"低级错误"),
    /**
     * 外部原因
     */
    EXTERNAL(21,"外部原因"),
    /**
     * 重复出现
     */
    REPEAT(22,"重复出现"),
    ;

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugReasonEnum e : BugReasonEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }
}
