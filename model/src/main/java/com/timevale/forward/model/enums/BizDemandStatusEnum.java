package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 */
@Getter
public enum BizDemandStatusEnum {
    /**
     * 业务需求状态
     */
    EVALUATE(0, "待评估"),
    RECEIVED(10, "已接收"),
    TO_CONFIRM(13, "待确认"),
    COMPLETED(15, "已完成无需开发"),
    PD_LINKED(17, "已关联产品需求"),
    INCLUDE_PROJECT(20, "已列入项目"),
    PROJECTING(30, "项目进行中"),
    AVAILABLE(40, "已完成上线"),
    REJECT(-10, "被驳回"),
    INVALID(-20, "已作废");

    private final Integer code;
    private final String text;

    BizDemandStatusEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (BizDemandStatusEnum e : BizDemandStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }

    public static Boolean statusNeedNotice(Integer code){
        return code.equals(INCLUDE_PROJECT.getCode()) || code.equals(PROJECTING.getCode()) || code.equals(AVAILABLE.getCode());
    }

}
