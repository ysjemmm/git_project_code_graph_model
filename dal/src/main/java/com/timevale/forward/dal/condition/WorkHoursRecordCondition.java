package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/3 15:55
 * @description: 工时记录查询条件
 */
@Data
@Builder
public class WorkHoursRecordCondition extends QueryBase {
    /**
     * id
     */
    private Long id;

    /**
     * ids
     */
    private List<Long> ids;

    /**
     * 项目ids
     */
    private List<Long> projectIds;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 工作项类型
     */
    private Integer workItemType;

    /**
     * 工作项id
     */
    private Long workItemId;

    /**
     * 工作项ids
     */
    private List<Long> workItemIds;

    /**
     * 创建人id
     */
    private String createManId;

    /**
     * 创建人ids
     */
    private List<String> createManIds;

    /**
     * 前一天的开始时间
     */
    private String stratTime;

    /**
     * 前一天的结束时间
     */
    private String endTime;

}
