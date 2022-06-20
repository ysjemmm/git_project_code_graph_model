package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("项目目标完成情况填写请求")
public class ProjectGoalFinishReq extends ProjectGoalIdReq {

    @Pattern(regexp = "^(10|30)$", message = "完成状态只能是 10-已完成 或 30-未完成")
    @NotNull(message = "完成状态必填")
    @ApiModelProperty("10-已完成 30-未完成")
    private Integer status;
    @ApiModelProperty("完成情况说明")
    private String completeNote;

}
