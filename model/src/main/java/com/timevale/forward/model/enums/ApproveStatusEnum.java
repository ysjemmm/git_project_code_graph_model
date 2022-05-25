package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ApproveStatusEnum {
    /**
     * 发布状态
     */
    NOT_STARTED("审核未开始"),

    WAITING_APPROVE("等待审批"),

    REJECTED("审批未通过"),

    APPROVED("审批通过");


    final private String text;

    ApproveStatusEnum(String text){
        this.text = text;
    }
    public static String getTextByName(String name){
        for (ApproveStatusEnum e : ApproveStatusEnum.values()){
            if(e.name().equals(name)){
                return e.text;
            }
        }
        return "无";
    }
}
