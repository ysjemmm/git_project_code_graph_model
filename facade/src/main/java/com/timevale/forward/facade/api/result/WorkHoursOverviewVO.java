package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:46
 * @description: 工作台任务工时
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("人员任务工时概览")
public class WorkHoursOverviewVO extends ToString {

    @ApiModelProperty("成员id")
    private String teamMemberId;

    @ApiModelProperty("成员名称")
    private String teamMemberName;

    @ApiModelProperty("未登记日期")
    private List<String> unregisteredDateList;

    @ApiModelProperty("工时记录")
    private List<WorkHoursTaskVO> workHoursTasks;
}
