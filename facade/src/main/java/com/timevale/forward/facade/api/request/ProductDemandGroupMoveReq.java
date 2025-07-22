package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 产品需求分组拖动请求
 * @author qiyuan
 * @date 2025/07/15 15:30
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组拖动请求")
public class ProductDemandGroupMoveReq extends BaseReq {

    @ApiModelProperty(value = "业务域id", required = true)
    @NotNull(message = "业务域id不能为空")
    private Long bizDomainId;

    @ApiModelProperty(value = "待拖动ID, mode为moveIn时为产品需求id，其他为分组需求id", required = true)
    @NotNull(message = "拖动ID不能为空")
    private Long id; // moveIn时时productDemandId， 其他时为productDemandGroupItemId

    @ApiModelProperty("前一个ID")
    private Long prevId;

    @ApiModelProperty("后一个ID")
    private Long nextId;



} 