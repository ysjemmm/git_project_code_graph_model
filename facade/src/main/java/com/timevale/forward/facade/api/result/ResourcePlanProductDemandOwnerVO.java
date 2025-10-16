package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author mayang
 * @date 2025-10-15 19:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组-资源规划-需求负责人")
public class ResourcePlanProductDemandOwnerVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("负责人id")
    private String ownerId;

    @ApiModelProperty("关联的需求id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productDemandId;

    @ApiModelProperty("关联的资源类型")
    private String resourceType;

    @ApiModelProperty("关联的需求评估（人天）")
    private BigDecimal resourceTime;
}
