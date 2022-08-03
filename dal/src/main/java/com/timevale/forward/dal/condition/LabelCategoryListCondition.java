package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class LabelCategoryListCondition extends QueryBase {

    /**
     * names
     */
    private String names;
    /**
     * types
     */
    private List<Integer> types;

    /**
     * bizDomainIds
     */
    private List<Long> bizDomainIds;

}
