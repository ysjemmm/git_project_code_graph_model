package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Date;

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

    @ApiModelProperty("里程碑所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段")
    private Integer stage;

    @ApiModelProperty("里程碑所属项目阶段")
    private String stageName;

    @ApiModelProperty("计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("负责人id")
    private String chargeManId;

    @ApiModelProperty("负责人")
    private String chargeMan;

    @ApiModelProperty("关键行动")
    private String keyAction;

    @ApiModelProperty("行动")
    private Collection<ProjectMilestoneActionVO> actions;

}
