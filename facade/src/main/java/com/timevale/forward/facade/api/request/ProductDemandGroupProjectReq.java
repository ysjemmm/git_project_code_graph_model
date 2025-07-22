package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 产品需求分组项目请求
 * @author qiyuan
 * @date 2025/07/15 15:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组项目请求")
public class ProductDemandGroupProjectReq extends ProductDemandGroupReq {
    @ApiModelProperty(value = "项目id", required = true)
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "绑定类型: true(绑定) false(取消绑定)", required = true)
    @NotNull(message = "绑定类型不能为空")
    private Boolean type;
}