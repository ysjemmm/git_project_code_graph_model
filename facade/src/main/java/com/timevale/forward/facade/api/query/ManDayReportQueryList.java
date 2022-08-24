package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;


/**
 * @author by YangXu
 * @date 2022/08/19 16:23
 */
@Data
@ApiModel("人天提报列表查询入参")
public class ManDayReportQueryList extends QueryBase {

    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty("提报状态")
    private List<Integer> auditStatuses;

    @ApiModelProperty("提报人")
    private List<String> createManIds;

    @ApiModelProperty("审批人")
    private List<String> pmIds;

    @ApiModelProperty("提报起始时间")
    private Date createStartDate;

    @ApiModelProperty("提报结束时间")
    private Date createEndDate;

    @ApiModelProperty(value = "日期范围: yyyy-MM-dd ~ yyyy-MM-dd")
    private String weekDateRange;

    @ApiModelProperty("驳回原因")
    private String rejectReason;

    @NotNull(message = "Tab标识必填")
    @ApiModelProperty(value = "提报tab标识：AUDIT待我审核的，REPORT我提报的人天，All全部人天", required = true)
    private String tabTag;
}
