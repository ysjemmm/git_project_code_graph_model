package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/03/23 14:29
 */
@Getter
@AllArgsConstructor
public enum BugOnlineReasonEnum {
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
            if (e.stage.getCode().equals(stage)) {
                result.add(e.getCode());
            }
        }
        return result;
    }

    public static BugOnlineReasonEnum getByCode(Integer code) {
        for (BugOnlineReasonEnum e : BugOnlineReasonEnum.values()) {
            if (e.code.equals(code)) {
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
                return e.getStage().getText() + "-" + e.getText();
            }
        }
        return "";
    }
}
