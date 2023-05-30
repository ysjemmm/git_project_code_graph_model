package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Date;

/**
 * @author jingchun
 */
@Getter
@Setter
public class ProjectMilestoneModifyReq extends ToString {

    @NotNull(message = "里程碑id必填")
    @ApiModelProperty("里程碑id")
    private Long id;

    @NotBlank(message = "里程碑名称")
    @ApiModelProperty("里程碑名称")
    private String milestoneName;

    @ApiModelProperty("里程碑所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段")
    private Integer stage;

    @ApiModelProperty("计划开始日期")
    @NotNull(message = "计划开始日期必填")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date planStartDate;

    @ApiModelProperty("计划结束日期")
    @NotNull(message = "计划结束日期必填")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date planEndDate;

    @ApiModelProperty("里程碑行动-任务")
    @Valid
    private Collection<TaskAddReq> tasks;

    @ApiModelProperty("里程碑行动-项目")
    private Collection<Long> relationIds;
}
