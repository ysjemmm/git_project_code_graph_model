package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 故障通知单二级分类
 *
 * @author yangxu
 * @date 2024/06/11
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketCategorySecondEnum {
    ENVIRONMENTAL_INSTALLATION(10001, "环境安装", TroubleTicketCategoryFirstEnum.DELIVER),
    SERVICE_DEPLOYMENT(10002, "服务部署", TroubleTicketCategoryFirstEnum.DELIVER),
    INITIALIZE_BUSINESS_CONFIGURATION(10003, "初始化业务配置", TroubleTicketCategoryFirstEnum.DELIVER),
    VERSION_UPGRADE(10004, "版本升级", TroubleTicketCategoryFirstEnum.DELIVER),

    CONFIGURATION_MANAGEMENT(20001, "配置管理", TroubleTicketCategoryFirstEnum.OPERATION),
    SYSTEM_CHANGES(20002, "系统变更", TroubleTicketCategoryFirstEnum.OPERATION),
    DATA_SERVICE_MIGRATION(20003, "数据/服务迁移", TroubleTicketCategoryFirstEnum.OPERATION),
    CUSTOMER_SCENE_CHANGE(20004, "客景变更", TroubleTicketCategoryFirstEnum.OPERATION),

    CODE_FUNCTIONALITY_ISSUES(30001, "代码功能问题", TroubleTicketCategoryFirstEnum.TEST),
    CODE_PERFORMANCE_ISSUES(30002, "代码性能问题", TroubleTicketCategoryFirstEnum.TEST),

    EXTERNAL_DEPENDENCIES(40001, "外部依赖", TroubleTicketCategoryFirstEnum.THIRD_PARTY),
    INTERNAL_DEPENDENCIES(40002, "内部依赖", TroubleTicketCategoryFirstEnum.THIRD_PARTY),

    SECURITY_ATTACK_AND_DEFENSE(50001, "安全攻防", TroubleTicketCategoryFirstEnum.SECURITY),
    PRODUCT_SAFETY(50002, "产品安全", TroubleTicketCategoryFirstEnum.SECURITY),

    RESOURCE_ANOMALY(60001, "资源异常", TroubleTicketCategoryFirstEnum.RESOURCE),

    ;

    private final Integer code;
    private final String text;
    private final TroubleTicketCategoryFirstEnum stage;
    private final static String SEPARATOR = "-";


    public static TroubleTicketCategorySecondEnum getByCode(Integer code) {
        for (TroubleTicketCategorySecondEnum e : TroubleTicketCategorySecondEnum.values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

    public static String getTextByCode(Integer code) {
        for (TroubleTicketCategorySecondEnum e : TroubleTicketCategorySecondEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }

    public static String getFullTextByCode(Integer code) {
        for (TroubleTicketCategorySecondEnum e : TroubleTicketCategorySecondEnum.values()) {
            if (e.code.equals(code)) {
                return e.getStage().getText() + TroubleTicketCategorySecondEnum.SEPARATOR + e.getText();
            }
        }
        return "";
    }
}
