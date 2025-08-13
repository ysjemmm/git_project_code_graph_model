package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class ProductDemandGroupQueryCondition extends QueryBase {

    /**
     * 产品需求条件
     */
    private ProductDemandListCondition condition;

    /**
     * 父产品需求条件
     */
    private ProductDemandGroupCondition parentCondition;

    /**
     * 分组条件
     */
    private String groupField;

    /**
     * 查询条件
     */
    private String selectField;

    /**
     * 排序条件
     */
    private String orderField;
}
