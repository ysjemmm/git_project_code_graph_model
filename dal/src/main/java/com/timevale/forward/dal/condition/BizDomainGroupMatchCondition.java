package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author qiyuan
 * @date 2025-08-25 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class BizDomainGroupMatchCondition extends QueryBase {

    /**
     * name
     */
    @WildcardEscape
    private String name;

    /**
     * bizDomainGroupId
     */
    private Long bizDomainGroupId;

}
