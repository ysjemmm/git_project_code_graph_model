package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author jingchun
 * created on 2023/2/10
 */
@Getter
@AllArgsConstructor
public enum ProjectDocumentTypeEnum {

    PRODUCT(1, "产品需求文档"),
    SET_UP(11, "立项申请报告"),
    SCHEME(12, "项目方案报告"),
    AUDIT(13, "审计计划"),
    REPLAY(14, "项目复盘报告"),
    OPERATION(15, "运营计划"),
    OTHER(16, "其他");

    private final Integer code;
    private final String text;

}
