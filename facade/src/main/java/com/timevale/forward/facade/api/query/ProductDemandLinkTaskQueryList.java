package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品-任务需求查询")
public class ProductDemandLinkTaskQueryList extends QueryBase {

    @ApiModelProperty("产品需求id")
    @NotNull(message = "产品需求id不能为空")
    private Long productDemandId;
}
