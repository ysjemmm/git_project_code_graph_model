package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author mayang
 * @date 2025-10-15 18:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@ApiModel("产品需求分组-资源规划信息")
public class ProductDemandGroupResourcePlanVO extends ToString {

    @ApiModelProperty("产品需求分组id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productDemandGroupId;

    @ApiModelProperty("是否有权限编辑，前端特有字段")
    private Boolean editable;

    @ApiModelProperty("关联的需求列表")
    private List<ResourcePlanProductDemandVO> productDemands;
}
