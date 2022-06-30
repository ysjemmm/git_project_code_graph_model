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
@ApiModel("埋点属性删除")
public class TrackPropDeleteReq extends BaseReq {

    @ApiModelProperty("属性id")
    @NotNull(message = "属性id不能为空")
    private Long id;

}
