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
 * @title: ProductDemandGroupItemVO
 * @Author qiyuan
 * @Date 2025/7/15 11:55
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组需求")
public class ProductDemandGroupItemVO extends ToString {
    @ApiModelProperty("主键id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @ApiModelProperty("产品需求分组id")
    private Long productDemandGroupId;
    @ApiModelProperty("产品需求id")
    private Long productDemandId;
    @ApiModelProperty("相对位置")
    private BigDecimal position;
    @ApiModelProperty("版本号")
    private Long version;
    @ApiModelProperty("产品需求")
    private ProductDemandVO productDemand;

    @ApiModelProperty("UED资源评估（人天）")
    private BigDecimal uedTime;

    @ApiModelProperty("后端资源评估（人天）")
    private BigDecimal backTime;

    @ApiModelProperty("前端资源评估（人天）")
    private BigDecimal frontTime;

    @ApiModelProperty("测试资源评估（人天）")
    private BigDecimal qaTime;

    @ApiModelProperty("功能迁移评估（人天）")
    private BigDecimal transferTime;

    @ApiModelProperty("产品资源评估（人天）")
    private BigDecimal productTime;

    @ApiModelProperty("总资源评估（人天）")
    private BigDecimal totalTime;
}
