package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: qiyuan
 * @create: 2025-08-13 11:10
 */
@Getter
@AllArgsConstructor
public enum ViewsTypeEnum {

    /**
     * 业务需求
     */
    BIZ_DEMAND(10,"业务需求"),

    /**
     * 产品需求
     */
    PRODUCT_DEMAND(11,"产品需求"),

    /**
     * 项目
     */
    PROJECT(12,"项目"),

    /**
     * 线下bug
     */
    BUG_OFFLINE(13,"线下bug"),

    /**
     * 线上bug
     */
    BUG_ONLINE(14,"线上bug"),

    /**
     * 任务
     */
    TASK(15, "任务");

    private final Integer code;
    private final String text;

    public static ViewsTypeEnum getByCode(Integer code){
        for (ViewsTypeEnum e : ViewsTypeEnum.values()){
            if(e.code.equals(code)){
                return e;
            }
        }
        return null;
    }

    public static boolean existCode(Integer code) {
        return getByCode(code) != null;
    }

    public static String getTextByCode(Integer code){
        for (ViewsTypeEnum e : ViewsTypeEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "";
    }
    public static String getTextByCode(List<Integer> codes){
        List<String>text=new ArrayList<>();
        for (Integer code : codes) {
            text.add(getTextByCode(code));
        }
        return StringUtils.join(text,",");
    }
}
