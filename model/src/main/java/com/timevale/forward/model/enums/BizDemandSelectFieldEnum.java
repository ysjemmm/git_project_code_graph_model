package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @auther: yuhua
 * @date: 2025/7/24 14:07
 * @description:
 */
@Getter
public enum BizDemandSelectFieldEnum {

    BIZ_DOMAIN("bizDomain","pl.biz_domain_id as bizDomainId", "业务域"),

    PRODUCT_LINE("productLine","bd.product_line_id as productLineId", "产品线"),

    LABEL_CATEGORY("labelCategory","f.label_category_id as labelCategoryId", "标签类别"),

    TARGET_CUSTOMER("targetCustomer","bd.target_customer as targetCustomer", "需求接收人"),

    STATUS("status","bd.status as status", "需求解决状态"),

    PRIORITY("priority","bd.priority as priority", "优先级"),

    DEMAND_DEPT("demandDept","bd.dept_id as deptId", "需求部门"),

    RECEIVE_MAN("receiveMan","bd.receive_man_id as receiveManId", "需求接收人");

    final private String selectField;

    final private String sourceField;

    final private String fieldChinese;

    BizDemandSelectFieldEnum(String selectField, String sourceField, String fieldChinese){
        this.selectField = selectField;
        this.sourceField = sourceField;
        this.fieldChinese = fieldChinese;
    }

    public static String getSourceFieldBySelectField(String selectField){
        for (BizDemandSelectFieldEnum e : BizDemandSelectFieldEnum.values()){
            if(e.getSelectField().equals(selectField)){
                return e.sourceField;
            }
        }
        return "";
    }

    public static String getFieldChineseBySelectField(String selectField){
        for (BizDemandSelectFieldEnum e : BizDemandSelectFieldEnum.values()){
            if(e.getSelectField().equals(selectField)){
                return e.fieldChinese;
            }
        }
        return "";
    }

    // 得到所有的selectFieldsKey
    public static String[] getAllSelectFieldsKey(){
        String[] selectFields = new String[BizDemandSelectFieldEnum.values().length];
        for (int i = 0; i < BizDemandSelectFieldEnum.values().length; i++) {
            selectFields[i] = BizDemandSelectFieldEnum.values()[i].getSelectField();
        }
        return selectFields;
    }
}
