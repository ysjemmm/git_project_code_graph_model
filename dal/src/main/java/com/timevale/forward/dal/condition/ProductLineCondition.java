package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class ProductLineCondition {

    /**
     * names
     */
    @WildcardEscape
    private String name;

    /**
     * ownerIds
     */
    private List<String> ownerIds;

    /**
     * bugOnlineOwnerIds
     */
    private List<String> bugOnlineOwnerIds;

    /**
     * 业务域id
     */
    private List<Long> bizDomainIds;

    /**
     * 产品线等级
     */
    private List<Integer> productLineLevels;

}
