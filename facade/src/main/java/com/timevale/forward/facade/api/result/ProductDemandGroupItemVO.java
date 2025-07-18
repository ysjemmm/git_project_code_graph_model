package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    private Double position;
    @ApiModelProperty("版本号")
    private Long version;
    @ApiModelProperty("产品需求")
    private ProductDemandVO productDemand;
}
