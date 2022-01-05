package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/01/05 10:17
 */
@Getter
public enum ProcessorEnum {
    // 处理器: 0 X86/X64, 1兆芯，2飞腾，3龙芯，4鲲鹏，5申威
    X86X64(0, "X86/X64"),
    VIA(1, "兆芯"),
    PHYTIUM(2, "飞腾"),
    LOONGSON(3, "龙芯"),
    KUNPENG(4, "鲲鹏"),
    SUNWAY(5, "申威");

    private final Integer code;
    private final String text;

    ProcessorEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (ProcessorEnum e : ProcessorEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
