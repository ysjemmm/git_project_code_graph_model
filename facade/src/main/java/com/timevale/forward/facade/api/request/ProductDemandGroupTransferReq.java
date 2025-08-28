package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 产品需求分组转交请求
 * @author qiyuan
 * @date 2025/08/25 15:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组转交请求")
public class ProductDemandGroupTransferReq extends ProductDemandGroupReq {
    @ApiModelProperty(value = "业务域集id", required = true)
    @NotNull(message = "业务域集id不能为空")
    private Long bizDomainGroupId;
}