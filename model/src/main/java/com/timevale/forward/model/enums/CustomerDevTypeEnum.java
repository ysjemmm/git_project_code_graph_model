package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/21 14:46
 */
@Getter
public enum CustomerDevTypeEnum {
    /**
     * 预期上线时间
     */
    CONTRACT(0, "基于销售合同"),
    
    PROJECT(1, "没有合同，基于项目");

    Integer code;
    String text;

    CustomerDevTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (CustomerDevTypeEnum e : CustomerDevTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
