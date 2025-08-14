package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
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

    private List<String> receiveManIds;

    private Long deptId;

    private Long labelCategoryId;

    private List<Long> labelCategoryIds;

    private String targetCustomer;
}
