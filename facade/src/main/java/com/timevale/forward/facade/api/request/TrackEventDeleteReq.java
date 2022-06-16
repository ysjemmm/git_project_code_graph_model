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
@ApiModel("埋点事件删除")
public class TrackEventDeleteReq extends BaseReq {

    @ApiModelProperty("操作类型:0删除,1撤回")
    @NotNull(message = "操作类型不能为空")
    private Integer type;

    @ApiModelProperty("事件id")
    @NotNull(message = "事件id不能为空")
    private Long id;

}
