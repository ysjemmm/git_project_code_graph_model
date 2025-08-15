package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;

/**
 * @auther: yuhua
 * @date: 2025/8/14 15:26
 * @description: 业务需求分组条件
 */
@Data
@Builder
public class BizDemandGroupQueryCondition extends QueryBase {

    /**
     * 业务需求条件
     */
    private BizDemandListCondition condition;

    /**
     * 父业务需求条件
     */
    private BizDemandGroupCondition parentCondition;

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
