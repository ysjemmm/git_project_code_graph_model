package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 18:54
 */
@Getter
@AllArgsConstructor
public enum BizDemandReasonEnum {
    /**
     * 驳回理由
     */
    BIZDEMAND_VALUE_WEAK(0,"需求业务价值较弱"),
    ALREADY_ALTERNATIVES(1,"需求已有替代方案可实现"),
    DESCRIPTION_NOT_CLEAR(2,"需求描述不清"),
    ALREADY_SUPPORT(3,"产品已支持"),
    REPEAT_SUBMIT(4,"重复提交"),
    ISSUER_ONLINE(5,"线上问题，请提交线上bug"),
    UNREASONABLE(6,"需求不合理"),
    UNABLE(7,"无法实现"),
    NO_RESPONSE_ON_DEMAND_SIDE(8, "需求方无响应");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code){
        for (BizDemandReasonEnum e : BizDemandReasonEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }

}
