package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/28 15:55
 */
@Getter
@AllArgsConstructor
public enum BugFieldEnum {

    PROJECTS("关联项目"),

    PRODUCT_LINE("关联产品线"),

    UN_HANDLE_REASON("不用修复原因"),

    DELAY_HANDLE_REASON("延期修复原因");

    private final String text;
}
