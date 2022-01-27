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
 * @date 2022/01/26 09:46
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-任务工时看板-项目时间段")
public class HomePageProjectDateVO extends ToString {

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("开始规划时间")
    private Date startPlan;

    @ApiModelProperty("需求内审时间")
    private Date demandInternalAudit;

    @ApiModelProperty("需求串讲时间")
    private Date demandConstrue;

    @ApiModelProperty("技术详设评审时间")
    private Date technicalDetailReview;

    @ApiModelProperty("开发开始时间")
    private Date developStart;

    @ApiModelProperty("测试用例编写时间")
    private Date writeTestCases;

    @ApiModelProperty("测试用例评审时间")
    private Date useCaseReview;

    @ApiModelProperty("提测时间")
    private Date submitTest;

    @ApiModelProperty("测试开始时间")
    private Date testStart;

    @ApiModelProperty("发布模拟时间")
    private Date publishSimulate;

    @ApiModelProperty("发布正式时间")
    private Date publishOfficial;

}
