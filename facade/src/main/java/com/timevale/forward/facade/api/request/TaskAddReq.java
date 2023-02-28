package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("任务新增")
public class TaskAddReq extends BaseReq {

    @ApiModelProperty("名称")
    @NotNull(message = "名称不能为空")
    private String name;

    @ApiModelProperty(value = "项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "产品线id")
    private Long productLineId = 0L;

    @ApiModelProperty(value = "0需求规划阶段,1研发阶段,2测试阶段")
    @NotNull(message = "阶段不能为空")
    private Integer stage;

    @ApiModelProperty("计划开始时间")
    @NotNull(message = "计划开始时间不能为空")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date planStartDate;

    @ApiModelProperty("计划结束时间")
    @NotNull(message = "计划结束时间不能为空")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date planEndDate;

    @ApiModelProperty("计划耗时")
    @NotNull(message = "计划耗时不能为空")
    private BigDecimal planUseTime;

    @ApiModelProperty("实际开始时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date actualStartDate;

    @ApiModelProperty("实际结束时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date actualEndDate;

    @ApiModelProperty(value = "执行人")
    @NotNull(message = "执行人不能为空")
    private List<PersonAddReq> executors;

    @ApiModelProperty("执行人等级:1-核心，2-扩展")
    private Integer executorLevel = 1;

    @ApiModelProperty(value = "产品需求id")
    private List<Long> productDemandIds;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;

    @ApiModelProperty("是否创建待办")
    @NotNull(message = "是否创建不能为空")
    private Boolean todo;

    @ApiModelProperty("任务描述")
    private String desc;

    @ApiModelProperty("是否为里程碑")
    private Boolean milestoneFlag = false;
}
