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

    @ApiModelProperty("总计工时")
    private BigDecimal actualHours;

    @ApiModelProperty("实际工时集合")
    private ActualHoursSeries actualHoursSeries;

    @ApiModelProperty("工作项信息")
    private ColumnField columnField;

    @Data
    public static class ActualHoursSeries extends ToString {

        @ApiModelProperty("日期时间轴")
        private List<String> times;

        @ApiModelProperty("工时轴")
        private List<BigDecimal> values;
    }

    @Data
    public static class ColumnField extends ToString {

        @ApiModelProperty("工作项类型")
        private Integer workItemType;

        @ApiModelProperty("名称")
        private String name;

        @ApiModelProperty("项目id")
        private Long projectId;

        @ApiModelProperty("工作项id")
        private Long workItemId;

    }
}
