package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/21 14:46
 */
@Getter
public enum PlanReleaseDateEnum {
    /**
     * 预期上线时间
     */
    Q1_EARLY(0, "1月"),
    Q1_MIDDLE(1, "2月"),
    Q1_LATE(2, "3月"),
    Q2_EARLY(3, "4月"),
    Q2_MIDDLE(4, "5月"),
    Q2_LATE(5, "6月"),
    Q3_EARLY(6, "7月"),
    Q3_MIDDLE(7, "8月"),
    Q3_LATE(8, "9月"),
    Q4_EARLY(9, "10月"),
    Q4_MIDDLE(10, "11月"),
    Q4_LATE(11, "12月"),
    UNABLE_EVALUATE(20, "暂时无法评估");

    Integer code;
    String text;

    PlanReleaseDateEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (PlanReleaseDateEnum e : PlanReleaseDateEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
