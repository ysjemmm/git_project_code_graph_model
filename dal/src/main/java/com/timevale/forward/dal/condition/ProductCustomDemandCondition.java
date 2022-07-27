package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

/**
 * @author by YangXu
 * @date 2021/12/16 14:07
 */
@Data
@Builder
public class ProductCustomDemandCondition {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 客户需求id
     */
    private Long customDemandId;

    /**
     * 逻辑删除标识
     */
    private Boolean isDeleted;

}
