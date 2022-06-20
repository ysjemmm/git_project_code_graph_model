package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("项目目标删除入参")
public class ProjectGoalIdReq extends BaseReq {
    @NotNull(message = "项目目标id必传")
    @ApiModelProperty(name = "项目目标id")
    private Long id;
}
