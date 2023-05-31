package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @author jingchun
 */
@Getter
@Setter
public class ProjectMilestoneActionVO extends ToString {

    @ApiModelProperty("主键id")
    private Long id;

    @ApiModelProperty("行动名称")
    private String name;

    @ApiModelProperty("行动类型0-任务;1-项目")
    private Integer type;

    @ApiModelProperty("行动状态")
    private Integer status;

    @ApiModelProperty("行动状态名称")
    private String statusName;

    @ApiModelProperty(value = "负责人")
    private String principal;

    @ApiModelProperty(value = "负责人id")
    private String principalId;

    @ApiModelProperty("计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("项目类型：0产研项目，1内部项目")
    private Integer category;

}
