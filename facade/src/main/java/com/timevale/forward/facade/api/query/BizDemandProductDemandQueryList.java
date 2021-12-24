package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/24 10:39
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求已关联产品需求查询")
public class BizDemandProductDemandQueryList extends QueryBase {

    @ApiModelProperty("业务需求id")
    private Long bizDemandId;
}
