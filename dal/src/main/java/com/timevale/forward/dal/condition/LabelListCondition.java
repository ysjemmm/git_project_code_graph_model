package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
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
public class LabelListCondition extends QueryBase {

    /**
     * names
     */
    @WildcardEscape
    private String name;

    /**
     * 保护方式 0-不保护 1-保护写入和删除
     */
    private Integer protection;

    /**
     * categoryNames
     */
    private List<String> categoryNames;
    /**
     * types
     */
    private List<Integer> types;

    /**
     * bizDomainIds
     */
    private List<Long> bizDomainIds;

    /**
     * createManId
     */
    private String createManId;

}
