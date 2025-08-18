package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @auther: yuhua
 * @date: 2025/7/24 14:07
 * @description:
 */
@Getter
public enum ProductSelectFieldEnum {

    BIZ_DOMAIN("bizDomain","d.id as bizDomainId", "业务域"),

    PRODUCT_LINE("productLine","c.id as productLineId", "产品线"),

    LABEL("label","e.label_id as labelId", "标签"),

    TYPE("type","a.type as type", "产品需求类型"),

    STATUS("status","a.status as status", "产品需求状态"),

    PRIORITY("priority","a.priority as priority", "优先级"),

    EXPECT_SCHEDULE_TIME("expectScheduleTime","DATE_FORMAT(a.expect_schedule_time, '%Y-%m') as expectScheduleTime", "需求排期时间"),

    OWNER("owner","a.owner_id as ownerId", "产品需求负责人");

    final private String selectField;

    final private String sourceField;

    final private String fieldChinese;

    ProductSelectFieldEnum(String selectField, String sourceField, String fieldChinese){
        this.selectField = selectField;
        this.sourceField = sourceField;
        this.fieldChinese = fieldChinese;
    }

    public static String getSourceFieldBySelectField(String selectField){
        for (ProductSelectFieldEnum e : ProductSelectFieldEnum.values()){
            if(e.getSelectField().equals(selectField)){
                return e.sourceField;
            }
        }
        return "";
    }

    public static String getFieldChineseBySelectField(String selectField){
        for (ProductSelectFieldEnum e : ProductSelectFieldEnum.values()){
            if(e.getSelectField().equals(selectField)){
                return e.fieldChinese;
            }
        }
        return "";
    }

    // 得到所有的selectFieldsKey
    public static String[] getAllSelectFieldsKey(){
        String[] selectFields = new String[ProductSelectFieldEnum.values().length];
        for (int i = 0; i < ProductSelectFieldEnum.values().length; i++) {
            selectFields[i] = ProductSelectFieldEnum.values()[i].getSelectField();
        }
        return selectFields;
    }
}
