package com.timevale.forward.model.enums;

import lombok.Getter;
import org.assertj.core.util.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * @auther: qiyuan
 * @date: 2025/7/24 14:07
 * @description:
 */
@Getter
public enum ViewsGroupFieldEnum {
    /**
     * 优先级:0(P0),10(P1),20(P2),30(P3)
     */
    BIZ_DOMAIN("bizDomain", "业务域", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND, ViewsTypeEnum.BUG_OFFLINE)),

    PRODUCT_LINE("productLine","产品线", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND, ViewsTypeEnum.BUG_OFFLINE)),

    PRIORITY("priority", "优先级", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND, ViewsTypeEnum.BUG_OFFLINE)),

    TYPE("type","产品需求类型", 0, Lists.newArrayList(ViewsTypeEnum.PRODUCT_DEMAND)),

    PRODUCT_DEMAND_STATUS("status", "产品需求状态", 0, Lists.newArrayList(ViewsTypeEnum.PRODUCT_DEMAND)),

    BIZ_DEMAND_STATUS("status", "需求解决状态", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND)),

    PRODUCT_DEMAND_OWNER("owner","产品需求负责人",0, Lists.newArrayList(ViewsTypeEnum.PRODUCT_DEMAND)),

    TARGET_CUSTOMER("targetCustomer","目标客户", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    DEMAND_DEPT("demandDept","需求部门", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND)),

    RECEIVE_MAN("receiveMan","需求接收人", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND)),

    CUSTOMER_GRADE("customerGrade", "客户等级", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    BUG_OFFLINE_STATUS("status", "Bug状态", 0, Lists.newArrayList(ViewsTypeEnum.BUG_OFFLINE)),

    BUG_OFFLINE_SEVERITY("severity", "严重程度", 0, Lists.newArrayList(ViewsTypeEnum.BUG_OFFLINE)),

    BUG_OFFLINE_OPERATOR("operator", "经办人", 0, Lists.newArrayList(ViewsTypeEnum.BUG_OFFLINE)),

    BUG_OFFLINE_PROPOSER("proposer", "提出人", 0, Lists.newArrayList(ViewsTypeEnum.BUG_OFFLINE)),

    LABEL_CATEGORY("labelCategory", "标签类别", 1, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND, ViewsTypeEnum.BUG_OFFLINE));


    final private String key;

    final private String name;

    final private int type; // 0:基础字段，1：标签类别

    final private List<ViewsTypeEnum> viewsTypes;

    ViewsGroupFieldEnum(String key,  String name, int type, List<ViewsTypeEnum> viewsTypes){
        this.key = key;
        this.name = name;
        this.type = type;
        this.viewsTypes = viewsTypes;
    }

    public static List<ViewsGroupFieldEnum> getByViewsType(ViewsTypeEnum viewsTypeEnum) {
        List<ViewsGroupFieldEnum> list = new ArrayList<>();
        for (ViewsGroupFieldEnum e : ViewsGroupFieldEnum.values()) {
            if (e.getViewsTypes() != null && e.getViewsTypes().contains(viewsTypeEnum)) {
                list.add(e);
            }
        }
        return list;
    }
}
