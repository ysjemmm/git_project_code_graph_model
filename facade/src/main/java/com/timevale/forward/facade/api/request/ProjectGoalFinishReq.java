package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("项目目标完成情况填写请求")
public class ProjectGoalFinishReq extends BaseReq {

    @NotNull(message = "项目目标id必传")
    @ApiModelProperty(name = "项目目标id")
    private Long id;

    @Pattern(regexp = "^(10|30)$", message = "完成状态只能是 10-已完成 或 30-未完成")
    @NotNull(message = "完成状态必填")
    @ApiModelProperty("10-已完成 30-未完成")
    private Integer status;

    @Length(max = 500, message = "完成情况说明不得超过500字")
    @ApiModelProperty("完成情况说明")
    private String completeNote;

}
