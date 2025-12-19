package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * @auther: yuhua
 * @date: 2025/10/29 11:28
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求状态更新")
public class ProductDemandStatusUpdateReq extends BaseReq {

    @ApiModelProperty("产品需求id")
    private Long id;

    @ApiModelProperty("产品需求状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @ApiModelProperty("待批量更新的产品需求id,与[id]参数仅能只填一种")
    private Set<Long> ids;
}
