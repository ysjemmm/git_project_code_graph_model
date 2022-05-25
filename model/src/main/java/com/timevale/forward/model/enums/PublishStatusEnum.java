package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum PublishStatusEnum {
    /**
     * 发布状态
     */
    NOT_STARTED("未开始"),

    RELEASING("正在发布"),

    FINISHED("已结束");


    final private String text;

    PublishStatusEnum(String text){
        this.text = text;
    }
    public static String getTextByName(String name){
        for (PublishStatusEnum e : PublishStatusEnum.values()){
            if(e.name().equals(name)){
                return e.text;
            }
        }
        return "无";
    }
}
