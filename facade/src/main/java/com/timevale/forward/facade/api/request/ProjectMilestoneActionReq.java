package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author jingchun
 */
@Getter
@Setter
public class ProjectMilestoneActionReq extends ToString {

    @NotNull(message = "请选择里程碑行动类型")
    @ApiModelProperty("里程碑行动类型0-任务;1-项目")
    private Integer type;

    @ApiModelProperty("关联数据id，项目id")
    private Long relationId;

    @ApiModelProperty("任务计划开始时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date planStartDate;

    @ApiModelProperty("任务计划结束时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date planEndDate;

    @ApiModelProperty(value = "任务执行人")
    private List<PersonAddReq> executors;

    @ApiModelProperty("是否创建待办")
    private boolean todo;

    @ApiModelProperty("任务描述")
    private String desc;
}
