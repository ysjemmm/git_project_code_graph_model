package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 18:54
 */
@Getter
@AllArgsConstructor
public enum CustomDemandReasonEnum {
    /**
     * 驳回理由
     */
    UNABLE(0,"无法实现"),

    FEELING_BAD(1,"情绪不满"),

    CUSTOMIZED_DEMAND (2,"定制需求");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code){
        for (CustomDemandReasonEnum e : CustomDemandReasonEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }

}
