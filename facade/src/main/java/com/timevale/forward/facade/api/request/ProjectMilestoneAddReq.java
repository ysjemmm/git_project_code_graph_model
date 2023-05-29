package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * @author jingchun
 */
@Getter
@Setter
public class ProjectMilestoneAddReq extends ToString {

    @NotNull(message = "里程碑所属项目必填")
    @ApiModelProperty("项目id")
    private Long projectId;

    @NotBlank(message = "里程碑名称")
    @ApiModelProperty("里程碑名称")
    private String milestoneName;

    @NotNull(message = "请选择里程碑类型")
    @ApiModelProperty("里程碑类型0-任务;1-项目")
    private Integer type;

    @ApiModelProperty("里程碑所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段")
    private Integer stage;

    @ApiModelProperty("里程碑行动-任务")
    @Valid
    private Collection<TaskAddReq> tasks;

    @ApiModelProperty("里程碑行动-关联id")
    private Collection<Long> relationIds;

}
