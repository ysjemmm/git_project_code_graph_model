package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author qiyuan
 * @date 2025-07-24 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求-项目关联")
public class ProductDemandGroupProjectLinkReq extends BaseReq {

    @ApiModelProperty(value = "项目id,关联时必填", required = true)
    private Long projectId;

    @NotNull(message = "产品需求分组id不能为空")
    @ApiModelProperty(value = "产品需求分组id", required = true)
    private Long productDemandGroupId;

    @ApiModelProperty(value = "0:关联,1:取消", required = true)
    @NotNull(message = "关联类型不能为空")
    private Integer type;
}
