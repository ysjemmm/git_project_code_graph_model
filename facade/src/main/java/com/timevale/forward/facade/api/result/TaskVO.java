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

    @ApiModelProperty("项目状态:0待执行、10进行中、20已完成、-10已暂停、-20已作废")
    private Integer status;

    @ApiModelProperty("项目状态")
    private String statusName;

    @ApiModelProperty("项目阶段:0需求规划阶段,1研发阶段,2测试阶段")
    private Integer stage;

    @ApiModelProperty("项目阶段")
    private String stageName;

    @ApiModelProperty("执行人")
    private String executor;

    @ApiModelProperty("项目计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("项目实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("项目实际结束时间")
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

}
