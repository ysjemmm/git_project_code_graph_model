package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    DEMAND_QUESTION(0, "需求问题",null),

    /**
     * 环境配置问题
     */
    ENV_CONFIGURE(1, "环境配置问题",null),

    /**
     * 功能错误
     */
    FUNCTIONAL_ERROR(2, "功能错误",null),

    /**
     * 兼容性问题
     */
    COMPATIBILITY_PROBLEM(3, "兼容性问题",null),

    /**
     * 数据问题
     */
    DATA_PROBLEM(4, "数据问题",null),

    /**
     * 性能问题
     */
    PERFORMANCE_PROBLEM(5, "性能问题",null),

    /**
     * 安全问题
     */
    SAFE_PROBLEM(6, "安全问题",null),

    /**
     * 外部原因
     */
    OUTSIDE_REASON(7, "外部原因",null),

    /**
     * 开发误操作
     */
    DEVELOP_MISTAKE_OPERATION(8, "开发误操作",null),

    /**
     * 接口文档编写错误
     */
    DOCUMENT_MISTAKE(9, "接口文档编写错误",null),

    /**
     * ISV问题
     */
    ISV(10, "ISV问题",null),

    /**
     * 历史版本
     */
    HISTORY_VERSION(11, "历史版本",null),

    /**
     * 无测试参与版本
     */
    NO_TEST_VERSION(12, "无测试参与版本",null),

    /**
     * 测试环境延期未修复bug
     */
    TEST_ENV_DELAY(13, "测试环境延期未修复bug",null),

    /**
     * 设计缺陷
     */
    DEGREE_DEFECT(14, "设计缺陷",null),

    /**
     * 定制版升级改动波及
     */
    CHANGE_AFFECT(15, "定制版升级改动波及",null),

    /**
     * 无法重现但客户环境偶现
     */
    CUSTOMER_ENV_OCCASION(16, "无法重现但客户环境偶现",null),

    /**
     * 无法重现但客户环境必现
     */
    CUSTOMER_ENV_MUST(17, "无法重现但客户环境必现",null),

    /**
     * 公有云问题
     */
    OPEN_CLOUD(18, "公有云问题",null),

    /**
     * 中间件问题
     */
    MIDDLEWARE(19, "中间件问题",null),

    /**
     * 产品设计问题
     */
    PRODUCT_DESIGN(20, "产品设计问题",null),

    /**
     * 用户体验
     */
    USER_EXPERIENCE(21, "用户体验",null),

    /**
     * 特殊文档问题
     */
    SPECIAL_DOC(22, "特殊文档问题",null),


    NULL(0, "空", BugOnlineReasonStageEnum.NULL),

    PRODUCT_DESIGN_FLAWS(10001, "产品设计缺陷", BugOnlineReasonStageEnum.PRODUCT_DESIGN),
    PERFORMANCE_UNDEFINED(10002, "性能标准未定义", BugOnlineReasonStageEnum.PRODUCT_DESIGN),
    COMPATIBILITY_UNDEFINE(10003, "兼容性标准未定义", BugOnlineReasonStageEnum.PRODUCT_DESIGN),
    SECURITY_UNDEFINE(10004, "安全性设计标准未定义", BugOnlineReasonStageEnum.PRODUCT_DESIGN),
    UI_DESIGN_FLAWS(10005, "UI交互设计缺陷", BugOnlineReasonStageEnum.PRODUCT_DESIGN),
    DELAY(10006, "问题延期修复", BugOnlineReasonStageEnum.PRODUCT_DESIGN),
    EXTERNAL_SERVICES(10007, "外部服务供应商原因", BugOnlineReasonStageEnum.PRODUCT_DESIGN),

    TECHNICAL_DESIGN_FLAWS(20001, "技术设计缺陷", BugOnlineReasonStageEnum.DEV),
    CHANGE_EVALUATION_INSUFFICIENT(20002, "改动评估不足", BugOnlineReasonStageEnum.DEV),
    EXTERNAL_TECHNICAL(20003, "外部技术供应商原因", BugOnlineReasonStageEnum.DEV),
    NEW_ISV(20004, "ISV原因", BugOnlineReasonStageEnum.DEV),
    UNKNOWN(20005, "原因未查明", BugOnlineReasonStageEnum.DEV),
    INTERFACE_DOC(20006, "接口文档编写错误", BugOnlineReasonStageEnum.DEV),
    DATA(20007, "数据原因", BugOnlineReasonStageEnum.DEV),
    SECURITY_FLAW(20008, "安全漏洞", BugOnlineReasonStageEnum.DEV),

    TEST_PLAN_MISSING(30001, "测试方案遗漏", BugOnlineReasonStageEnum.TEST),
    CASE_MISSING(30002, "用例缺失", BugOnlineReasonStageEnum.TEST),
    CASE_NON_EXECUTION(30003, "用例未执行", BugOnlineReasonStageEnum.TEST),
    NO_REPETITION(30004, "未复现问题", BugOnlineReasonStageEnum.TEST),
    CHANGE_NO_TEST(30005, "代码变更未测试", BugOnlineReasonStageEnum.TEST),

    SERVICE_INTERRUPT(40001, "服务中断", BugOnlineReasonStageEnum.PUBLISH),
    PUBLISH_PLAN(40002, "发布计划问题", BugOnlineReasonStageEnum.PUBLISH),
    ENVIRONMENT_CONFIG(40003, "环境配置导致", BugOnlineReasonStageEnum.PUBLISH),
    PUBLISH_MISOPERATION(40004, "误操作", BugOnlineReasonStageEnum.PUBLISH),
    NOTICE_MISSING(40005, "产品培训或发布公告缺失", BugOnlineReasonStageEnum.PUBLISH),

    PRODUCT_INCOMPREHENSION(50001, "对产品的理解错误或不熟悉", BugOnlineReasonStageEnum.OPERATION),
    PRODUCT_VERSION(50002, "产品的版本错误", BugOnlineReasonStageEnum.OPERATION),
    CONFIG(50003, "配置错误", BugOnlineReasonStageEnum.OPERATION),
    SQL(50004, "SQL执行错误或遗漏", BugOnlineReasonStageEnum.OPERATION),
    NO_FEEDBACK(50005, "长时间未反馈", BugOnlineReasonStageEnum.OPERATION),
    INFO_INVALID(50006, "提供信息无法用于排查", BugOnlineReasonStageEnum.OPERATION),
    SELF_SOLVE(50007, "已自助排查解决", BugOnlineReasonStageEnum.OPERATION),
    DOC_ERROR(50008, "文档与实际功能不符", BugOnlineReasonStageEnum.OPERATION),
    CLIENT_DATA(50009, "客户侧数据导致", BugOnlineReasonStageEnum.OPERATION),
    CLIENT_ENVIRONMENT(50010, "客户侧环境导致", BugOnlineReasonStageEnum.OPERATION),
    CLIENT_OPERATION(50011, "客户使用操作错误", BugOnlineReasonStageEnum.OPERATION),
    CLIENT_INCOMPREHENSION(50012, "客户对业务理解错误", BugOnlineReasonStageEnum.OPERATION),
    CLIENT_COORDINATE(50013, "客户配合度不够", BugOnlineReasonStageEnum.OPERATION),

    TECHNICAL_SUPPORT(90001, "技术咨询或支持", BugOnlineReasonStageEnum.OTHER),
    REPEAT_SUBMIT(90002, "重复提交", BugOnlineReasonStageEnum.OTHER),

    ;

    private final Integer code;
    private final String text;
    private final BugOnlineReasonStageEnum stage;

    public static List<Integer> getByStage(Integer stage) {
        List<Integer> result = new ArrayList<>();
        for (BugOnlineReasonEnum e : BugOnlineReasonEnum.values()) {
            if (Objects.equals(stage, e.stage.getCode())) {
                result.add(e.getCode());
            }
        }
        return result;
    }

    public static BugOnlineReasonEnum getByCode(Integer code) {
        for (BugOnlineReasonEnum e : BugOnlineReasonEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    public static String getTextByCode(Integer code) {
        for (BugOnlineReasonEnum e : BugOnlineReasonEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }

    public static String getFullTextByCode(Integer code) {
        for (BugOnlineReasonEnum e : BugOnlineReasonEnum.values()) {
            if (e.code.equals(code)) {
                return e.getStage().getText() + "/" + e.getText();
            }
        }
        return "";
    }
}
