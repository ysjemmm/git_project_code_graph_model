package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点新增")
public class ProjectNodeAddReq extends BaseReq{

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("计划完成开始时间")
    private Date planDate;

    @ApiModelProperty("计划完成结束时间")
    private Date planEndDate;

    @ApiModelProperty("实际完成开始时间")
    private Date actualDate;

    @ApiModelProperty("实际完成结束时间")
    private Date actualEndDate;

    @ApiModelProperty("阶段类型")
    private String stageType;
}
