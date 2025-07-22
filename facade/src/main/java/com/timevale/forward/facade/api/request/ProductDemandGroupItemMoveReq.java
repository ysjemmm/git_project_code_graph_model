package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 产品需求分组需求移动请求
 * @author qiyuan
 * @date 2025/07/15 15:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组需求移动请求")
public class ProductDemandGroupItemMoveReq extends ProductDemandGroupMoveReq {
    @ApiModelProperty("需求移动类型")
    @NotNull(message = "mode不能为空")
    private String mode; // follow: 分组之间拖动， moveIn: 待规划拖进来，moveOut: 拖到待规划

    @ApiModelProperty("目标分组id")
    @NotNull(message = "目标分组id不能为空")
    private Long targetGroupId;
}