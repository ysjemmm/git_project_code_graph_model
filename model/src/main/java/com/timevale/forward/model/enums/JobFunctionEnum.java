package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/10 15:30
 */
@Getter
public enum JobFunctionEnum {
    /**
     * 产品
     */
    PD("产品","产品"),
    /**
     * 测试
     */
    QA("测试","测试"),
    /**
     * 后端开发
     */
    BACK_RD("后端开发","开发"),
    /**
     * 前端开发
     */
    FRONT_RD("前端开发","开发"),
    /**
     * 运维
     */
    OP("运维","开发"),
    /**
     * DBA
     */
    DBA("DBA","开发"),
    /**
     * 安全
     */
    SAFE("安全","开发"),
    /**
     * 大数据
     */
    BIG_DATA("大数据","开发"),
    /**
     * 技术
     */
    TECHNICIAN("技术","开发"),
    /**
     * 技术专家
     */
    TECHNICAL_EXPERT("技术专家","开发");

    private final String name;
    private final String type;

    JobFunctionEnum(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public static String getType(String name){
        for (JobFunctionEnum e : JobFunctionEnum.values()){
            if(e.name.equals(name)){
                return e.type;
            }
        }
        return "其他";
    }

}
