package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;

/**
 * 线下Bug分组查询条件（包装类）
 */
@Data
@Builder
public class BugOfflineGroupQueryCondition extends QueryBase {

    /**
     * 线下Bug查询条件
     */
    private BugOfflineListCondition condition;

    /**
     * 父分组查询条件
     */
    private BugOfflineGroupCondition parentCondition;

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
