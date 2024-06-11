package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 故障通知单一级分类
 *
 * @author yangxu
 * @date 2024/06/11
 */
@Getter
@AllArgsConstructor
public enum TroubleTicketCategoryFirstEnum {
    DELIVER(10000, "交付"),
    OPERATION(20000, "运维"),
    TEST(30000, "研发"),
    THIRD_PARTY(40000, "第三方"),
    SECURITY(50000, "安全"),
    RESOURCE(60000, "资源"),
    ;

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (TroubleTicketCategoryFirstEnum e : TroubleTicketCategoryFirstEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
