package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * 线下Bug分组字段枚举
 */
@Getter
public enum BugOfflineGroupFieldEnum {

    BIZ_DOMAIN("bizDomain", "bd.id", "业务域"),

    PRODUCT_LINE("productLine", "bo.product_line_id", "产品线"),

    LABEL("label", "e.label_id", "标签类别"),

    STATUS("status", "bo.status", "Bug状态"),

    PRIORITY("priority", "bo.priority", "优先级"),

    SEVERITY("severity", "bo.severity", "严重程度"),

    OPERATOR("operator", "bo.operator_id", "经办人"),

    PROPOSER("proposer", "bo.proposer_id", "提出人");

    final private String groupField;

    final private String sourceField;

    final private String fieldChinese;

    BugOfflineGroupFieldEnum(String groupField, String sourceField, String fieldChinese) {
        this.groupField = groupField;
        this.sourceField = sourceField;
        this.fieldChinese = fieldChinese;
    }

    public static String getSourceFieldByGroupField(String groupField) {
        for (BugOfflineGroupFieldEnum e : BugOfflineGroupFieldEnum.values()) {
            if (e.getGroupField().equals(groupField)) {
                return e.sourceField;
            }
        }
        return "";
    }

    public static String getFieldChineseByGroupField(String groupField) {
        for (BugOfflineGroupFieldEnum e : BugOfflineGroupFieldEnum.values()) {
            if (e.getGroupField().equals(groupField)) {
                return e.fieldChinese;
            }
        }
        return "";
    }

    public static String[] getAllGroupFieldsKey() {
        String[] groupFields = new String[BugOfflineGroupFieldEnum.values().length];
        for (int i = 0; i < BugOfflineGroupFieldEnum.values().length; i++) {
            groupFields[i] = BugOfflineGroupFieldEnum.values()[i].getGroupField();
        }
        return groupFields;
    }
}
