package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;


/**
 * @author by YangXu
 * @date 2022/05/25 16:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目维度看板-数据指标")
public class ProjectBoardDataIndicatorVO extends ToString {

    @ApiModelProperty("项目任务进展")
    private BigDecimal taskProgress;

    @ApiModelProperty("总产品需求数")
    private Integer productDemandCount;

    @ApiModelProperty("总任务数")
    private Integer taskCount;

    @ApiModelProperty("总线下bug数")
    private Integer bugOfflineCount;

    @ApiModelProperty("提测结果")
    private String submitTestResult;

    @ApiModelProperty("逾期任务数")
    private Integer overdueTaskCount;

    @ApiModelProperty("待完成任务")
    private Integer waitingTaskCount;

    @ApiModelProperty("今日应完成任务")
    private Integer completeTaskToday;

    @ApiModelProperty("今日待完成任务")
    private Integer completeTaskTodayRemain;

    @ApiModelProperty("为拆解任务需求数")
    private Integer notDismantleDemand;

    @ApiModelProperty("待处理项目风险数")
    private Integer waitingRiskCount;

    @ApiModelProperty("待开发解决线下bug数")
    private Integer waitingSolveBugOfflineCount;

    @ApiModelProperty("待验证线下bug数")
    private Integer waitingCheckBugOfflineCount;

    @ApiModelProperty("延期修复线下bug数")
    private Integer postponeRepairBugOfflineCount;
}
