package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author jingchun
 * created on 2023/2/8
 */
@Getter
@Setter
@ApiModel("项目阶段变更请求")
public class ProjectStageChangeReq extends ToString {

    @NotNull(message = "项目id必填")
    @ApiModelProperty(value = "项目id", required = true)
    private Long projectId;

    @NotNull(message = "枚举必填")
    @ApiModelProperty(value = "阶段枚举", required = true)
    private Integer stage;

}
