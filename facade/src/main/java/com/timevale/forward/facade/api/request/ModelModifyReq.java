package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("模块修改")
public class ModelModifyReq extends ModelAddReq {

    @ApiModelProperty("模块id")
    @NotNull(message = "模块id不能为空")
    private Long id;

}
