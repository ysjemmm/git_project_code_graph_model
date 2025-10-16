package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
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
@ApiModel("产品需求分组-资源规划-需求")
public class ResourcePlanProductDemandAddReq extends ToString {

    @ApiModelProperty(value = "产品需求id", required = true)
    @NotNull(message = "产品需求id不能为空")
    private Long productDemandId;

    @ApiModelProperty(value = "产品需求负责人集合", required = true)
    private List<ProductDemandOwnerAddReq> productDemandOwners;
}
