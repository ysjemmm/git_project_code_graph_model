package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:46
 * @description: 工作台任务工时
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工作台任务工时")
public class WorkbenchesWorkHoursVO extends ToString {

    @ApiModelProperty("项目")
    private ProjectVO project;

    @ApiModelProperty("项目成员工时集合")
    private List<TeamMemberHours> memberHoursList;

    @Data
    public static class TeamMemberHours extends ToString {

        @ApiModelProperty("成员id")
        private String teamMemberId;

        @ApiModelProperty("成员名称")
        private String teamMemberName;

        @ApiModelProperty("昨日投入工时")
        private BigDecimal yesterdayHours;

        @ApiModelProperty("今日投入工时")
        private BigDecimal todayHours;

        @ApiModelProperty("累计投入工时")
        private BigDecimal totalHours;
    }
}
