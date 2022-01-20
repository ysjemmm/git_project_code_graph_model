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
@ApiModel("首页-任务工时看板-项目时间段")
public class HomePageProjectTimeVO extends ToString {

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("项目名称")
    private String name;

    @ApiModelProperty("开始规划时间")
    private Date planDate;

    @ApiModelProperty("需求内审时间")
    private Date demandCheckDate;

    @ApiModelProperty("需求串讲时间")
    private Date demandAnalyseDate;

    @ApiModelProperty("技术评审时间")
    private Date techCheckDate;

    @ApiModelProperty("开发开始时间")
    private Date devStartDate;

    @ApiModelProperty("测试用例编写时间")
    private Date testCaseWriteDate;

    @ApiModelProperty("测试用例评审时间")
    private Date testCaseCheckDate;

    @ApiModelProperty("提测时间")
    private Date testExecDate;

    @ApiModelProperty("测试开始时间")
    private Date testStartDate;

    @ApiModelProperty("发布模拟时间")
    private Date preStartDate;

}
