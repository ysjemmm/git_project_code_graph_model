package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @auther: yuhua
 * @date: 2025/7/24 14:07
 * @description:
 */
@Getter
public enum GroupFieldEnum {
    /**
     * 优先级:0(P0),1(P1),2(P2)
     */
    BIZ_DOMAIN("bizDomain","d.id", "业务域"),

    PRODUCT_LINE("productLine","c.id", "产品线"),

    LABEL_CATEGORY("labelCategory","label_category_id", "标签类别"),

    TYPE("type","a.type", "产品需求类型"),

    STATUS("status","a.status", "产品需求状态"),

    PRIORITY("priority","a.priority", "优先级"),

    EXPECT_SCHEDULE_TIME("expectScheduleTime","DATE_FORMAT(a.expect_schedule_time, '%Y-%m')", "需求排期时间"),

    OWNER("owner","a.owner_id", "产品需求负责人");

    final private String groupField;

    final private String sourceField;

    final private String fieldChinese;

    GroupFieldEnum(String groupField, String sourceField, String fieldChinese){
        this.groupField = groupField;
        this.sourceField = sourceField;
        this.fieldChinese = fieldChinese;
    }

    public static String getSourceFieldByGroupField(String groupField){
        for (GroupFieldEnum e : GroupFieldEnum.values()){
            if(e.getGroupField().equals(groupField)){
                return e.sourceField;
            }
        }
        return "";
    }

    public static String getFieldChineseByGroupField(String groupField){
        for (GroupFieldEnum e : GroupFieldEnum.values()){
            if(e.getGroupField().equals(groupField)){
                return e.fieldChinese;
            }
        }
        return "";
    }

    // 得到所有的groupFieldsKey
    public static String[] getAllGroupFieldsKey(){
        String[] groupFields = new String[GroupFieldEnum.values().length];
        for (int i = 0; i < GroupFieldEnum.values().length; i++) {
            groupFields[i] = GroupFieldEnum.values()[i].getGroupField();
        }
        return groupFields;
    }
}
