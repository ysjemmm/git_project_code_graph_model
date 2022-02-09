package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("任务详情")
public class TaskDetailVO extends ToString {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目经理")
    private String pmId;

    @ApiModelProperty(value = "0需求规划阶段,1研发阶段,2测试阶段")
    private Integer stage;

    @ApiModelProperty(value = "阶段")
    private String stageName;

    @ApiModelProperty("计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("计划耗时")
    private BigDecimal planUseTime;

    @ApiModelProperty("实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("任务耗时")
    private BigDecimal taskUseTime;

    @ApiModelProperty(value = "执行人")
    private List<PersonVO> executors;

    @ApiModelProperty(value = "产品需求id")
    private List<Long> productDemandIds;

    @ApiModelProperty("文件信息")
    private List<FileVO> files;

    @ApiModelProperty("是否创建待办")
    private Boolean todo;

    @ApiModelProperty("状态:0待执行、10进行中、20已完成、-10已暂停、-20已作废")
    private Integer status;

    @ApiModelProperty("状态")
    private String statusName;

    @ApiModelProperty("产品线")
    private ProductLineVO productLineVO;

    @ApiModelProperty("人员耗时")
    private List<TaskTimeVO> taskTimeVO;


}
