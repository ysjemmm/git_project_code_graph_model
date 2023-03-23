package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/28 15:08
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求关联产品需求")
public class BizDemandLinkProductDemandReq extends BaseReq {

    @ApiModelProperty("业务需求Id")
    @NotNull(message = "业务需求id不能为空")
    Long id;

    @ApiModelProperty("关联产品需求Id")
    @NotEmpty(message = "产品需求id必填")
    List<Long> productDemandIdList;
}
