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
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("任务列表")
public class TaskVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("projectId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目经理")
    private String pmId;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("任务状态:0待执行、10进行中、20已完成、-10已暂停、-20已作废")
    private Integer status;

    @ApiModelProperty("任务状态")
    private String statusName;

    @ApiModelProperty("任务所属阶段:0需求规划阶段,1研发阶段,2测试阶段")
    private Integer stage;

    @ApiModelProperty("任务所属阶段")
    private String stageName;

    @ApiModelProperty("执行人")
    private String executor;

    @ApiModelProperty("执行人id")
    private String executorId;

    @ApiModelProperty("任务计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("任务计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("任务实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("任务实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("创建人id")
    private String createManId;
    
    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("是否延期")
    private Boolean isDelay;

    @ApiModelProperty("归属主体：0-产研项目，1-内部项目")
    private Integer category;

    @ApiModelProperty("当前登陆人是否为PMO")
    private Boolean isPMO;

    @ApiModelProperty("项目负责人")
    private String principal;

    @ApiModelProperty("项目负责人id")
    private String principalId;

    @ApiModelProperty("1-n负责人")
    private String otnPrincipal;

    @ApiModelProperty("1-n负责人Id")
    private String otnPrincipalId;

    @ApiModelProperty("任务类型：0-调研，1-详细设计，2-测试用例设计，3-开发，4-集测开发，5-code review，6-测试，7-线下bug修复，8-发布，9-线上bug修复")
    private Integer type;
}
