package com.timevale.forward.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author by YangXu
 * @date 2022/02/10 15:30
 */
@Getter
public enum JobFunctionEnum {
    /**
     * 产品
     */
    PD("产品",UserTypeEnum.PD),

    /**
     * 商业分析
     */
    BUSINESS_ANALYSIS("商业分析", UserTypeEnum.PD),

    /**
     * 测试
     */
    QA("测试",UserTypeEnum.QA),

    /**
     * 后端开发
     */
    BACK_RD("后端开发",UserTypeEnum.RD),

    /**
     * 前端开发
     */
    FRONT_RD("前端开发",UserTypeEnum.RD),

    /**
     * 运维
     */
    OP("运维",UserTypeEnum.RD),

    /**
     * DBA
     */
    DBA("DBA",UserTypeEnum.RD),

    /**
     * 安全
     */
    SAFE("安全",UserTypeEnum.RD),

    /**
     * 大数据
     */
    BIG_DATA("大数据",UserTypeEnum.RD),

    /**
     * 技术支持
     */
    TECHNICIAN("技术支持",UserTypeEnum.RD),


    /**
     * 经营管理
     */
    Manager("经营管理",UserTypeEnum.MANAGER),

    /**
     * 职能管理
     */
    FUNCTION("职能管理", UserTypeEnum.MANAGER),


    /**
     * 技术专家
     */
    TECHNICAL_EXPERT("技术专家",UserTypeEnum.RD);

    private final String name;
    private final UserTypeEnum type;

    JobFunctionEnum(String name, UserTypeEnum type) {
        this.name = name;
        this.type = type;
    }

    public static UserTypeEnum getType(String name){
        if(StringUtils.isEmpty(name)){
            return UserTypeEnum.OTHER;
        }
        for (JobFunctionEnum e : JobFunctionEnum.values()){
            if(e.name.equals(name)){
                return e.type;
            }
        }
        return UserTypeEnum.OTHER;
    }

}
