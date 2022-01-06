package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/01/05 10:37
 */
@Getter
public enum BizDomainTypeEnum {
    // 产品线类型 0：默认产  1：金格
    DEFAULT(0, "默认业务域"),
    KINGGRID(1, "金格业务域");

    private final Integer code;
    private final String text;

    BizDomainTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (BizDomainTypeEnum e : BizDomainTypeEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
