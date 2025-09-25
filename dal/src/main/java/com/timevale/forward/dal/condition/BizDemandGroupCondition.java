package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class BizDemandGroupCondition extends QueryBase {

    private Integer priority;

    private Long bizDomainId;

    private Long productLineId;

    private Long subProductLineId;

    private Integer status;

    private String receiveManId;

    private List<String> notInReceiveManIds;

    private Long deptId;

    private List<Long> labelIds;

    private List<Long> notInLabelIds;

    /**
     *包含的id
     */
    private List<Long> containIds;

    /**
     * 不包含的id
     */
    private List<Long> exclusiveIds;

    private List<Long> labelCategoryIds;

    private String targetCustomer;

    private String customerGrade;
}
