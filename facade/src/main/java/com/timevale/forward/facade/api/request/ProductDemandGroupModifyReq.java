package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 产品需求分组修改请求
 * @author qiyuan
 * @date 2025/07/14 15:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组修改请求")
public class ProductDemandGroupModifyReq extends ProductDemandGroupAddReq {

    @ApiModelProperty(value = "主键id", required = true)
    @NotNull(message = "主键id不能为空")
    private Long id;
} 