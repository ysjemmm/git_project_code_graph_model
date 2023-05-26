package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * @author jingchun
 */
@Getter
@Setter
public class ProjectMilestoneVO extends ToString {

    @ApiModelProperty("主键id")
    private Long id;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("里程碑名称")
    private String milestoneName;

    @ApiModelProperty("里程碑类型0-任务;1-项目")
    private Integer type;

    @ApiModelProperty(value = "执行人id")
    private String executorId;

    @ApiModelProperty(value = "执行人")
    private String executor;

    @ApiModelProperty("里程碑所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段")
    private Integer stage;

    @ApiModelProperty("项目类型: 0-产研项目; 1-内部项目")
    private Integer category;

    @ApiModelProperty("里程碑所属项目阶段")
    private String stageName;

    @ApiModelProperty("里程碑状态")
    private Integer status;

    @ApiModelProperty("里程碑状态名称")
    private String statusName;

    @ApiModelProperty("关联数据id")
    private Long relationId;

    @ApiModelProperty("项目/任务计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("项目/任务计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("项目/任务实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("项目/任务实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("关联数据名称")
    private String relationName;

    @ApiModelProperty("行动")
    private List<ProjectMilestoneActionVO> actions;

}
