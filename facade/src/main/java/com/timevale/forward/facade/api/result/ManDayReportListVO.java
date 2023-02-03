package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;


/**
 * @author by YangXu
 * @date 2022/08/19 16:12
 */
@Getter
@Setter
@ApiModel("人天提报列表展示类")
@Accessors(chain = true)
public class ManDayReportListVO extends ToString {

    @ApiModelProperty("提报id")
    private Long id;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("周开始日期")
    private Date weekStartDate;

    @ApiModelProperty("周结束日期")
    private Date weekEndDate;

    @ApiModelProperty("周期时间范围")
    private String weekDateRange;

    @ApiModelProperty("审批人（取项目最新pm）")
    private String pm;

    @ApiModelProperty("审批人id（取项目最新pm）")
    private String pmId;

    @ApiModelProperty("审核人")
    private String auditor;

    @ApiModelProperty("审核人id")
    private String auditorId;

    @ApiModelProperty("提报时间")
    private Date createDate;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("提报人天")
    private BigDecimal auditManDay;

    @ApiModelProperty("工时填报说明")
    private String manDayDesc;

    @ApiModelProperty("提报状态：0审核通过，10审核中，20已驳回")
    private Integer auditStatus;

    @ApiModelProperty("提报状态描述")
    private String auditStatusText;

    @ApiModelProperty("驳回原因")
    private String rejectReason;

    @ApiModelProperty("提报人")
    private String reportor;

    @ApiModelProperty("提报人id")
    private String reportorId;
}
