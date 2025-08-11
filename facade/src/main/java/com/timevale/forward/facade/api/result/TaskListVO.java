package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/04/25 18:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目-任务列表")
public class TaskListVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("执行人")
    private String executor;

    @ApiModelProperty("任务状态:0待执行、10进行中、20已完成、-10已暂停、-20已作废")
    private Integer status;

    @ApiModelProperty("任务状态名称")
    private String statusName;

    @ApiModelProperty("计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目类型:0-产研，1-核心")
    private Integer category;

    @ApiModelProperty("任务类型：0-其他，1-调研，2-详细设计，3-测试用例设计，4-开发，5-集测开发，6-code review，7-测试，8-线下bug修复，9-发布，10-线上bug修复")
    private Integer type;
}
