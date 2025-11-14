package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @auther: yuhua
 * @date: 2025/9/28 10:21
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求组内新增")
public class ProductDemandGroupInnerAddReq extends ProductDemandAddReq {

    @ApiModelProperty(value = "目标分组id, mode为moveOut时，传当前的分组id， 其他传具体的分组id", required = true)
    @NotNull(message = "目标分组id不能为空")
    private Long targetGroupId;

    @ApiModelProperty(value = "业务域集id", required = true)
    @NotNull(message = "业务域集id不能为空")
    private Long bizDomainGroupId;

    @ApiModelProperty("后一个ID")
    private Long nextId;
}
