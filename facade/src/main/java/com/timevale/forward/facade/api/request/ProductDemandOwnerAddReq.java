package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author mayang
 * @date 2025-10-15 10:58
 **/
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@ApiModel("产品需求分组-资源规划-需求负责人")
public class ProductDemandOwnerAddReq extends ToString {

    @NotNull(message = "关联需求id不能为空")
    @ApiModelProperty(value = "关联需求id", required = true)
    @EqualsAndHashCode.Include
    private Long productDemandId;

    @NotNull(message = "关联负责人不能为空")
    @ApiModelProperty(value = "关联负责人", required = true)
    private String owner;

    @NotNull(message = "关联负责人id不能为空")
    @ApiModelProperty(value = "关联负责人id", required = true)
    @EqualsAndHashCode.Include
    private String ownerId;

    @NotNull(message = "资源类型不能为空")
    @ApiModelProperty(value = "资源类型，前端固定值", required = true)
    @EqualsAndHashCode.Include
    private String resourceType;
}
