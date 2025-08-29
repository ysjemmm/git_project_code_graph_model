package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 产品需求分组新增请求
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组新增请求")
public class ProductDemandGroupAddReq extends BaseReq {

    @ApiModelProperty(value = "名称", required = true)
    @NotEmpty(message = "名称不能为空")
    private String name;

    @ApiModelProperty(value = "业务域集id", required = true)
    @NotNull(message = "业务域集id不能为空")
    private Long bizDomainGroupId;

    @ApiModelProperty(value = "需求负责人", required = true)
    @NotNull(message = "需求负责人不能为空")
    private PersonAddReq owner;

} 