package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Description: 修改业务域上架状态请求对象
 * @ClassName: UpdateBizDomainListingStatusReq
 * @Author: shaoye
 * @Date: 2023-08-23 11:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("修改业务域上架状态请求对象")
public class UpdateBizDomainListingStatusReq extends BaseReq {

    @ApiModelProperty(value = "业务域ID", required = true)
    @NotNull(message = "业务域ID不能为空")
    private Long bizDomainId;

    @ApiModelProperty(value = "上架状态：0-未上架，1-已上架", required = true)
    @NotNull(message = "上架状态不能为空")
    private Integer listingStatus;

}
