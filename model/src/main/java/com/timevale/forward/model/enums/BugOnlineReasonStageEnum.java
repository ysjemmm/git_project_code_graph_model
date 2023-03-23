package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2023/03/22 13:49
 */
@Getter
@AllArgsConstructor
public enum BugOnlineReasonStageEnum {

    NULL(0, "空"),
    PRODUCT_DESIGN(10000, "产品设计阶段"),
    DEV(20000, "研发阶段"),
    TEST(30000, "测试阶段"),
    PUBLISH(40000, "发布阶段"),
    OPERATION(50000, "运维/运营阶段"),
    OTHER(90000, "其他"),

    ;

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineReasonStageEnum e : BugOnlineReasonStageEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
