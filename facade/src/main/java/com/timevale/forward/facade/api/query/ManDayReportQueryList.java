package com.timevale.forward.facade.api.query;

import com.timevale.forward.facade.api.request.BaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;


/**
 * @author by YangXu
 * @date 2022/08/19 16:23
 */
@Data
@ApiModel("人天提报列表查询入参")
public class ManDayReportQueryList extends BaseReq {

    @ApiModelProperty(value = "项目id")
    private Long projectId;

    @ApiModelProperty("提报状态")
    private Integer auditStatus;

    @ApiModelProperty("提报人")
    private String createMandId;

    @ApiModelProperty("审批人")
    private String pmId;

    @ApiModelProperty("提报起始时间")
    private Date createStartDate;

    @ApiModelProperty("提报结束时间")
    private Date createEndDate;

    @ApiModelProperty("时间(周)开始日期")
    private Date weekStartDate;

    @ApiModelProperty("时间(周)结束日期")
    private Date weekEndDate;

    @NotNull(message = "Tab标识必填")
    @ApiModelProperty(value = "提报tab标识：AUDIT待我审核的，REPORT我提报的人天，All全部人天", required = true)
    private String tabTag;
}
