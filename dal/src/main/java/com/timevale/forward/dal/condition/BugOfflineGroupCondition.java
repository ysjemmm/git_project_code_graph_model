package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 线下Bug分组查询条件（父分组筛选）
 */
@Data
@Builder
public class BugOfflineGroupCondition extends QueryBase {

    private Integer priority;

    private Long bizDomainId;

    private Long productLineId;

    private Integer status;

    private Integer severity;

    private String operatorId;

    private List<String> notInOperatorIds;

    private String proposerId;

    private List<String> notInProposerIds;

    private List<Long> labelIds;

    private List<Long> notInLabelIds;

    /**
     * 包含的id
     */
    private List<Long> containIds;

    /**
     * 不包含的id
     */
    private List<Long> exclusiveIds;

    private List<Long> labelCategoryIds;

    /**
     * 包含的bug id（标签过滤后）
     */
    private List<Long> inBugOfflineIds;

    /**
     * 不包含的bug id（标签过滤后）
     */
    private List<Long> notInBugOfflineIds;
}
