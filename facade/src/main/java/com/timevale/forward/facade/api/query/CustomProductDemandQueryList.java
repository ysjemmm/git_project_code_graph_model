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
@ApiModel("客户需求-产品需求列表查询")
public class CustomProductDemandQueryList extends QueryBase {

    @NotNull(message = "客户需求id不能为空")
    @ApiModelProperty("客户需求id")
    private Long customDemandId;

}
