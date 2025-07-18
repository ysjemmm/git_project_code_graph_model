package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @ApiModelProperty("名称")
    @NotNull(message = "名称不能为空")
    private String name;

    @ApiModelProperty("业务域id")
    @NotNull(message = "业务域id不能为空")
    private Long bizDomainId;

    @ApiModelProperty("需求负责人")
    @NotNull(message = "需求负责人不能为空")
    private PersonAddReq owner;

} 