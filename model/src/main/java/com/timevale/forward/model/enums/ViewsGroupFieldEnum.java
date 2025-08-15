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
     * 优先级:0(P0),1(P1),2(P2)
     */
    BIZ_DOMAIN("bizDomain", "业务域", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    PRODUCT_LINE("productLine","产品线", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    TYPE("type","产品需求类型", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    STATUS("status", "产品需求状态", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    PRIORITY("priority", "优先级", 0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    OWNER("owner","产品需求负责人",0, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND)),

    LABEL_CATEGORY("labelCategory", "标签类别", 1, Lists.newArrayList(ViewsTypeEnum.BIZ_DEMAND, ViewsTypeEnum.PRODUCT_DEMAND));

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
