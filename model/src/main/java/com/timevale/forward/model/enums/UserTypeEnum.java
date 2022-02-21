package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/01/24 15:13
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {
    /**
     * 产品
     */
    PD(0,"产品"),

    /**
     * 开发
     */
    RD(1,"开发"),

    /**
     * 测试
     */
    QA(2,"测试"),

    /**
     * 经营管理
     */
    MANAGER(3,"经营管理"),

    /**
     * 其他
     */
    OTHER(-1,"其他");

    private final Integer code;
    private final String type;

    public static UserTypeEnum getByCode(Integer code){
        for (UserTypeEnum e : UserTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e;
            }
        }
        return UserTypeEnum.OTHER;
    }

}
