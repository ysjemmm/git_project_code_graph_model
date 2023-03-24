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

    DELAY_HANDLE_REASON("延期修复原因"),

    OPERATOR("经办人"),

    REPAIR_FAIL_REASON("修复失败原因"),

    DISMISS_CAUSE("驳回原因"),

    REASON("bug原因"),

    LABEL("标签"),

    LINK_BUG_ONLINE("关联线上bug"),

    LINK_BUG_OFFLINE("关联线下bug"),

    MODEL("模块");

    private final String text;
}
