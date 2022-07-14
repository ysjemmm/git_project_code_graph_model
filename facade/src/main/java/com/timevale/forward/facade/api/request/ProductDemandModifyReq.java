package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求修改")
public class ProductDemandModifyReq extends ProductDemandAddReq {

    @ApiModelProperty("id")
    @NotNull(message = "产品需求id不能为空")
    private Long id;

    @Valid
    @ApiModelProperty("需求变更请求")
    private ProductDemandDescChangeReq descChangeReq;

}
