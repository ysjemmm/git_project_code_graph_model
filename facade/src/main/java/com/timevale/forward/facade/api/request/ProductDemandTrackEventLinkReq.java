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
@ApiModel("产品-事件关联")
public class ProductDemandTrackEventLinkReq extends BaseReq {

    @NotNull(message = "产品需求id不能为空")
    @ApiModelProperty("产品需求id")
    private Long productDemandId;

    @NotNull(message = "事件id不能为空")
    @ApiModelProperty("事件id")
    private List<Long> trackEventIds;

    @ApiModelProperty("0:关联,1:取消")
    private Integer type;
}
