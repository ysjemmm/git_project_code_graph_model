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
@ApiModel("项目目标修改接口")
public class ProjectGoalModifyReq extends ProjectGoalAddReq {

    @NotNull(message = "项目目标id必传")
    @ApiModelProperty("项目目标主键id")
    private Long id;

}
