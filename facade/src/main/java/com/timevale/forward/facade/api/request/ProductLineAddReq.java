package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品线新增")
public class ProductLineAddReq extends BaseReq {

    @ApiModelProperty("业务域id")
    @NotNull(message = "业务域id不能为空")
    private Long bizDomainId;

    @ApiModelProperty("名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @ApiModelProperty("负责人")
    @NotBlank(message = "负责人不能为空")
    private String owner;

    @ApiModelProperty("负责人id")
    @NotBlank(message = "负责人id不能为空")
    private String ownerId;

    @ApiModelProperty("线上bug负责人")
    @NotBlank(message = "线上bug负责人不能为空")
    private String bugOnlineOwner;

    @ApiModelProperty("线上bug负责人id")
    @NotBlank(message = "线上bug负责人id不能为空")
    private String bugOnlineOwnerId;

    @ApiModelProperty("sr专家")
    private String srExpert;

    @ApiModelProperty("sr专家id")
    private String srExpertId;

}
