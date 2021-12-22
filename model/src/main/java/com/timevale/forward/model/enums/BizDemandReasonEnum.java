package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 18:54
 */
@Getter
public enum BizDemandReasonEnum {
    // 需求业务价值较弱
    BIZDEMAND_VALUE_WEAK(0),
    // 需求已有替代方案可实现
    ALREADY_ALTERNATIVES(1),
    // 需求描述不清
    DESCRIPTION_NOT_CLEAR(2),
    // 产品已支持
    ALREADY_SUPPORT(3),
    // 重复提交
    REPEAT_SUBMIT(4),
    // 线上问题，请提交线上bug
    ISSUER_ONLINE(5);

    private Integer code;

    BizDemandReasonEnum(Integer code){this.code = code;}

    public static String getTextByCode(Integer code){
        for (BizDemandReasonEnum e : BizDemandReasonEnum.values()){
            if(e.getCode().equals(code)){
                return e.toString();
            }
        }
        return "errorCode";
    }

}
