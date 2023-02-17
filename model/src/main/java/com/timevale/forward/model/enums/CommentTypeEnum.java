package com.timevale.forward.model.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2021/12/21 18:17
 */
@Getter
@AllArgsConstructor
public enum CommentTypeEnum {
    NULL(-1, "errorCode", TabEnum.NULL),
    // 主体类型:
    // 0项目
    PROJECT(0,"项目", TabEnum.PROJECT_MANAGEMENT),
    // 1产品需求
    PRODUCT_DEMAND(1,"产品需求", TabEnum.PRODUCT_MANAGEMENT),
    // 2业务需求
    BIZ_DEMAND(2,"业务需求", TabEnum.BUSINESS_MANAGEMENT),
    // 3任务
    TASK(3,"任务", TabEnum.TASK_MANAGEMENT),
    // 4线下bug
    BUG_OFFLINE(4,"线下bug", TabEnum.BUG_MANAGEMENT),
    // 5线上bug
    BUG_ONLINE(5,"线上bug", TabEnum.BUG_ONLINE_MANAGEMENT),
    // 6故障单
    TROUBLE_TICKET(6,"故障单", TabEnum.TROUBLE_MANAGEMENT),
    // 7 客户需求
    CUSTOM_DEMAND(7,"客户需求", TabEnum.CUSTOM_MANAGEMENT),
    // 8 内部项目
    INNER_PROJECT(8,"内部项目", TabEnum.INNER_PROJECT_MANAGEMENT),
    // 9 内部任务
    INNER_TASK(9, "内部任务", TabEnum.INNER_TASK_MANAGEMENT),
    ;

    private final Integer code;
    private final String text;
    private final TabEnum refTab;

    private static final Map<Integer, CommentTypeEnum> MAP =
            Maps.uniqueIndex(Arrays.asList(values()), CommentTypeEnum::getCode);

    public static CommentTypeEnum getByCode(Integer code) {
        return MAP.getOrDefault(code, NULL);
    }

    public static String getTextByCode(Integer code) {
        return getByCode(code).text;
    }

}
