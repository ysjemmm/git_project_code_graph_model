package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品-客户需求关联")
public class CustomProductDemandLinkReq extends BaseReq {

    @NotNull(message = "产品需求id不能为空")
    @ApiModelProperty("产品需求id")
    private List<Long> productDemandIds;

    @NotNull(message = "客户需求id不能为空")
    @ApiModelProperty("客户需求id")
    private Long customDemandId;

    @ApiModelProperty("0:关联,1:取消")
    private Integer type;
}
