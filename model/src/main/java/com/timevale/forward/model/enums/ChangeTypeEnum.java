package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by xingyun
 * @date 2022/01/05 10:17
 */
@Getter
public enum ChangeTypeEnum {
    // 0需求变更、1资源不足、2优先级降低、3外部依赖、9其他
    DEMAND_CHANGE(0, "需求变更"),
    RESOURCE_LESS(1, "资源不足"),
    PRIORITY_REDUCE(2, "优先级降低"),
    OUTSIDE_RELYON(3, "外部依赖"),
    OTHER(9, "其他");

    private final Integer code;
    private final String text;

    ChangeTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (ChangeTypeEnum e : ChangeTypeEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
