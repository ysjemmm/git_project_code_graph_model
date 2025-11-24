package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @auther: mayang
 * @date: 2025/11/24 11:28
 * @description: 需求优先级变更
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求优先级变更")
public class ProductDemandPriorityUpdateReq extends BaseReq {

    @ApiModelProperty("产品需求id")
    @NotNull(message = "产品需求id不能为空")
    private Long id;

    @ApiModelProperty("产品需求优先级")
    @NotNull(message = "优先级不能为空")
    private Integer priority;
}
