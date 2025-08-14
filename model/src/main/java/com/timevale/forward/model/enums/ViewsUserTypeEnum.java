package com.timevale.forward.model.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

/**
 * @author: qiyuan
 * @create: 2025-08-13 11:10
 **/
@Getter
@AllArgsConstructor
public enum ViewsUserTypeEnum {
    /**
     * 类型：0-自己创建的，1-他人分享的
     */
    MINE(0, "自己创建的"),

    SHARE(1, "他人分享的");

    private final Integer code;
    private final String text;

    // 枚举项较多，初始化时放入map提升性能
    private static final Map<Integer, ViewsUserTypeEnum> MAP =
            Maps.uniqueIndex(Arrays.asList(values()), ViewsUserTypeEnum::getCode);

    public static ViewsUserTypeEnum getByCode(Integer code) {
        return MAP.getOrDefault(code, MINE);
    }

    public static String getTextByCode(Integer code) {
        return getByCode(code).getText();
    }

}
