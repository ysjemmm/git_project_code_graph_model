package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author mayang
 * @date 2025-10-15 10:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组-资源规划")
public class ProductDemandGroupResourcePlanReq extends BaseReq {

    @NotNull(message = "业务域id不能为空")
    @ApiModelProperty(value = "业务域id", required = true)
    private Long bizDomainGroupId;

    @NotNull(message = "产品需求分组id不能为空")
    @ApiModelProperty(value = "产品需求分组id", required = true)
    private Long productDemandGroupId;

    @NotNull(message = "变更操作人id不能为空")
    @ApiModelProperty(value = "变更操作人id", required = true)
    private String operatorId;

    @NotNull(message = "变更操作人不能为空")
    @ApiModelProperty(value = "变更操作人", required = true)
    private String operator;

    @ApiModelProperty(value = "资源规划内产品需求信息列表")
    private List<ResourcePlanProductDemandAddReq> productDemands;
}
