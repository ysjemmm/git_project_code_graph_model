package com.timevale.forward.facade.api;

import com.timevale.forward.facade.api.request.BaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Description: 修改产品线上架状态请求对象
 * @ClassName: UpdateProductLineListingStatusReq
 * @Author: shaoye
 * @Date: 2023-08-23 13:37
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("修改产品线上架状态请求对象")
public class UpdateProductLineListingStatusReq extends BaseReq {

    @ApiModelProperty(value = "产品线ID", required = true)
    @NotNull(message = "产品线ID不能为空")
    private Long productLineId;

    @ApiModelProperty(value = "上架状态：0-未上架，1-已上架", required = true)
    @NotNull(message = "上架状态不能为空")
    private Integer listingStatus;

}
