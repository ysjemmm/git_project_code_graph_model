package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author jingchun
 */
@Getter
@Setter
public class ProjectMilestoneActionDelReq extends ToString {
    @NotNull(message = "行动id不能为空")
    @ApiModelProperty("行动id")
    private Long id;

    @NotNull(message = "行动类型不能为空")
    @ApiModelProperty("行动类型: 0-任务，1-项目")
    private Integer type;
}
