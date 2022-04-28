package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/04/24 17:23
 */
@Getter
@AllArgsConstructor
public enum ProjectRiskStatusEnum {

    INVALID(-1, "作废"),

    PENDING(0, "待处理"),

    COMPLETE(1, "已处理");

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code){
        for (ProjectRiskStatusEnum e : ProjectRiskStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
