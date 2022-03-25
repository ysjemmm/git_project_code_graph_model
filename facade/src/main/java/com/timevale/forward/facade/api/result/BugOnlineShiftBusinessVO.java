package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Date 2022/3/25 10:40
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug转业务需求")
public class BugOnlineShiftBusinessVO extends ToString {
    @ApiModelProperty("bug id")
    private Long bugOnlineId;

    @ApiModelProperty("线上bug的名字")
    private String bugOnlineName;

    @ApiModelProperty("上一个状态：0问题上报，1待确认，2关闭，3问题确认，4问题修复，5QA修复确认，6待上线，7挂起，8完成，9已转需求")
    private Integer prevStatus;
}