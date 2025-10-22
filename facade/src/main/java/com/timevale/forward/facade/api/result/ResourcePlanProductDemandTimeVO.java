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
 * @date 2025-10-22 10：58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组-资源规划-需求人天信息")
public class ResourcePlanProductDemandTimeVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("UED资源评估（人天）")
    private BigDecimal uedTime;

    @ApiModelProperty("后端资源评估（人天）")
    private BigDecimal backTime;

    @ApiModelProperty("前端资源评估（人天）")
    private BigDecimal frontTime;

    @ApiModelProperty("测试资源评估（人天）")
    private BigDecimal qaTime;

    @ApiModelProperty("产品资源评估（人天）")
    private BigDecimal productTime;

    @ApiModelProperty("运维迁移评估（人天）")
    private BigDecimal opsTime;

    @ApiModelProperty("安全资源评估（人天）")
    private BigDecimal securityTime;

}
