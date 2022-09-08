package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务域新增")
public class BizDomainAddReq extends BaseReq {

    @ApiModelProperty("名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @ApiModelProperty("负责人")
    @NotBlank(message = "负责人不能为空")
    private String owner;

    @ApiModelProperty("负责人id")
    @NotBlank(message = "负责人id不能为空")
    private String ownerId;

}
