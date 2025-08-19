package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @auther: yuhua
 * @date: 2025/7/24 14:07
 * @description:
 */
@Getter
public enum BizDemandGroupFieldEnum {

    BIZ_DOMAIN("bizDomain","pl.biz_domain_id", "业务域"),

    PRODUCT_LINE("productLine","bd.product_line_id", "产品线"),

    LABEL("label","e.label_id", "标签类别"),

    STATUS("status","bd.status", "需求解决状态"),

    PRIORITY("priority","bd.priority", "优先级"),

    TARGET_CUSTOMER("targetCustomer","bd.target_customer", "目标客户"),

    DEMAND_DEPT("demandDept","bd.dept_id", "需求部门"),

    RECEIVE_MAN("receiveMan","bd.receive_man_id", "需求接收人");

    final private String groupField;

    final private String sourceField;

    final private String fieldChinese;

    BizDemandGroupFieldEnum(String groupField, String sourceField, String fieldChinese){
        this.groupField = groupField;
        this.sourceField = sourceField;
        this.fieldChinese = fieldChinese;
    }

    public static String getSourceFieldByGroupField(String groupField){
        for (BizDemandGroupFieldEnum e : BizDemandGroupFieldEnum.values()){
            if(e.getGroupField().equals(groupField)){
                return e.sourceField;
            }
        }
        return "";
    }

    public static String getFieldChineseByGroupField(String groupField){
        for (BizDemandGroupFieldEnum e : BizDemandGroupFieldEnum.values()){
            if(e.getGroupField().equals(groupField)){
                return e.fieldChinese;
            }
        }
        return "";
    }

    // 得到所有的groupFieldsKey
    public static String[] getAllGroupFieldsKey(){
        String[] groupFields = new String[BizDemandGroupFieldEnum.values().length];
        for (int i = 0; i < BizDemandGroupFieldEnum.values().length; i++) {
            groupFields[i] = BizDemandGroupFieldEnum.values()[i].getGroupField();
        }
        return groupFields;
    }
}
