package com.timevale.forward.model.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 @Getter
 */
@Getter
public enum PlatformTypeEnum {
    /**
     * 项目流程状态
     */
    IOS(0, "IOS"),

    ANDROID(1, "Android"),

    JAVASCRIPT(2, "JavaScript"),

    MINI_PROGRAMS(3, "小程序"),

    SERVER(4, "服务端"),

    OTHER(9, "其他");
    private final Integer code;
    private final String text;

    PlatformTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (PlatformTypeEnum e : PlatformTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }

    public static List<String> getTextByCode(List<Integer> codes){
        List<String>result=new ArrayList<>();
        codes.forEach(a->{
            result.add(getTextByCode(a));
        });
        return result;
    }
}
