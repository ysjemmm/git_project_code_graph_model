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
@ApiModel("标签修改")
public class LabelModifyReq extends BaseReq {

    @ApiModelProperty("标签名称")
    @NotBlank(message = "标签名称不能为空")
    private String name;

    @ApiModelProperty("标签id")
    @NotNull(message = "标签id不能为空")
    private Long id;


}
