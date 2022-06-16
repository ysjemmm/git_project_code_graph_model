package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 */
@Getter
public enum TrackPropTypeEnum {
    /**
     * 属性类型
     */
    NEW(0, "新增属性"),

    EXIST(1, "已有属性"),

    DEFAULT(2, "默认属性");
    private final Integer code;
    private final String text;

    TrackPropTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (TrackPropTypeEnum e : TrackPropTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
