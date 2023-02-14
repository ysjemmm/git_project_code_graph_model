package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author jingchun
 * created on 2023/2/10
 */
@Getter
@AllArgsConstructor
public enum ProjectDocumentTypeEnum {

    NULL(0, "未知"),
    PRODUCT(1, "产品需求文档"),
    SET_UP(11, "立项申请报告"),
    SCHEME(12, "项目方案报告"),
    AUDIT(13, "审计计划"),
    REPLAY(14, "项目复盘报告"),
    OPERATION(15, "运营计划"),
    OTHER(16, "其他");

    private final Integer code;
    private final String text;

    public static ProjectDocumentTypeEnum getByCode(Integer code) {
        for (ProjectDocumentTypeEnum value : values()) {
            if (Objects.equals(value.code, code)) {
                return value;
            }
        }
        return NULL;
    }

    public static String getTextByCode(Integer code) {
        return getByCode(code).text;
    }

}
