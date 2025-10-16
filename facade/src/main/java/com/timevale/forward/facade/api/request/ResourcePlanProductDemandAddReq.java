package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author mayang
 * @date 2025-10-15 10:58
 **/
@Data
@ApiModel("产品需求分组-资源规划")
public class ResourcePlanProductDemandAddReq {

    @ApiModelProperty(value = "产品需求id", required = true)
    @NotNull(message = "产品需求id不能为空")
    private Long productDemandId;

    @ApiModelProperty(value = "产品需求负责人集合", required = true)
    private List<ProductDemandOwnerAddReq> productDemandOwners;
}
