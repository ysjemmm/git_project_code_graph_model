package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/22 16:07
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求关联产品需求")
public class LinkOrUnLinkProductDemandReq extends BaseReq {

    @ApiModelProperty("业务需求Id")
    Long id;

    @ApiModelProperty("关联产品需求Id")
    List<Long> productIdList;
}
