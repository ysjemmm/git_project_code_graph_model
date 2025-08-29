package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author qiyuan
 * @date 2025-08-25 13:58
 **/
@Data
@Builder
public class BizDomainGroupCondition {

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
     * 上架状态：0-未上架，1-已上架
     */
    private Integer listingStatus;

}
