package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/01/05 09:57
 */
@Getter
public enum OsEnum {
    // 操作系统:0 XP, 1 Win7,2 Win8，3 Win10，4中标麒麟，5银河麒麟，6麒麟V10，7中科方德，8统信UOS
    XP(0, "XP"),
    WIN7(1, "Win7"),
    WIN8(2, "Win8"),
    WIN10(3, "Win10"),
    NEOKYLIN(4, "中标麒麟"),
    KYLIN(5, "银河麒麟"),
    KYLINV10(6, "麒麟V10"),
    DELIX(7, "中科方德"),
    UOS(8, "统信UOS");

    private Integer code;
    private String text;

    OsEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (OsEnum e : OsEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
