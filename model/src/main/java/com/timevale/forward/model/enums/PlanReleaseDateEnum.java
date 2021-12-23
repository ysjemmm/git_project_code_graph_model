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
    Q1_EARLY(0, "Q1上旬"),
    Q1_MIDDLE(1, "Q1中旬"),
    Q1_LATE(2, "Q1下旬"),
    Q2_EARLY(3, "Q2上旬"),
    Q2_MIDDLE(4, "Q2中旬"),
    Q2_LATE(5, "Q2下旬"),
    Q3_EARLY(6, "Q3上旬"),
    Q3_MIDDLE(7, "Q3中旬"),
    Q3_LATE(8, "Q3下旬"),
    Q4_EARLY(9, "Q4上旬"),
    Q4_MIDDLE(10, "Q4中旬"),
    Q4_LATE(11, "Q4下旬"),
    UNABLE_EVALUATE(12, "暂时无法评估");

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
        return "errorCode";
    }
}
