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
@ApiModel("埋点事件修改")
public class TrackEventModifyReq extends TrackEventAddReq {

    @ApiModelProperty("埋点事件id")
    @NotNull(message = "埋点事件id不能为空")
    private Long id;

    @ApiModelProperty("埋点说明id")
    private String explanation;
}
