package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 产品需求分组需求拖动请求
 * @author qiyuan
 * @date 2025/07/15 15:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组需求拖动请求")
public class ProductDemandGroupItemMoveReq extends ProductDemandGroupMoveReq {
    @ApiModelProperty(value = "需求拖动类型, follow: 分组之间拖动， moveIn： 待规划需求拖进来， moveOut: 需求拖到待规划列表", required = true)
    @NotEmpty(message = "mode不能为空")
    private String mode;

    @ApiModelProperty(value = "目标分组id, mode为moveOut时，传当前的分组id， 其他传具体的分组id", required = true)
    @NotNull(message = "目标分组id不能为空")
    private Long targetGroupId;
}