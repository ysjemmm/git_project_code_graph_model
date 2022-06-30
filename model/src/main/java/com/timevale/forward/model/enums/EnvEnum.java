package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/02/23 17:05
 */
@Getter
@AllArgsConstructor
public enum EnvEnum {

    /**
     * 项目环境
     */
    PROJECT(0,"项目环境"),

    /**
     * 测试环境
     */
    TEST(1,"测试环境"),

    /**
     * 模拟环境
     */
    PREPARE(2,"模拟环境"),

    /**
     * 生产环境
     */
    PRODUCTION(3,"生产环境")

    ;

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code){
        for (EnvEnum e : EnvEnum.values()){
            if(e.code.equals(code)){
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
