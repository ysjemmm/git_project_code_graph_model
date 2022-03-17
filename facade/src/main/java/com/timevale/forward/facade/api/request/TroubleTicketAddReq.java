package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.ImprovementMeasureVO;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/16 14:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("故障工单-新增")
public class TroubleTicketAddReq extends BaseReq {

    @NotNull(message = "故障概述不能为空")
    @ApiModelProperty("故障概述")
    private String name;

    @ApiModelProperty("故障影响")
    private String influence;

    @NotNull(message = "故障类型不能为空")
    @ApiModelProperty("故障类型 0 业务故障，1 数据故障")
    private Integer type;

    @NotNull(message = "处理人不能为空")
    @ApiModelProperty("处理人")
    private String handler;

    @NotNull(message = "处理人id不能为空")
    @ApiModelProperty("处理人id")
    private String handlerId;

    @NotNull(message = "产品线不能为空")
    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("影响客户数（家）")
    private Integer influenceCount;

    @ApiModelProperty("受影响客户")
    private String influenceClient;

    @NotNull(message = "故障发生时间不能为空")
    @ApiModelProperty("故障发生时间")
    private Date occurrenceTime;

    @ApiModelProperty("业务恢复时间")
    private Date restoreTime;

    @ApiModelProperty("故障持续时间 0 ≤5分钟，1 5~20分钟，2 20~30分钟，3 30~60分钟，4 ＞60分钟")
    private Integer duringTime;

    @ApiModelProperty("不可用时长（分钟）")
    private BigDecimal failureTime;

    @ApiModelProperty("故障影响面分布 0 全网，1 单业务域，2 跨多个业务域")
    private Integer influenceScope;

    @ApiModelProperty("故障原因分布 0 功能问题，1 性能问题，2 数据问题，3 环境问题，4 安全问题，5 外部问题")
    private Integer reason;

    @ApiModelProperty("复盘时间")
    private Date replayTime;

    @ApiModelProperty("是否有资损")
    private Boolean assetLoss;

    @ApiModelProperty("损失金额（元）")
    private BigDecimal amountLoss;

    @ApiModelProperty("时间线")
    private String timeLine;

    @ApiModelProperty("原因分析")
    private String reasonAnalysis;

    @ApiModelProperty("故障定级 0 P0， 10 P1，20 P2， -10 未达到级别")
    private Integer troubleRank;

    @ApiModelProperty("主责任人")
    private String primePrincipal;

    @ApiModelProperty("主责任人id")
    private String primePrincipalId;

    @ApiModelProperty("次责任人")
    private String minorPrincipal;

    @ApiModelProperty("次责任人id")
    private String minorPrincipalId;

    @ApiModelProperty("责任团队id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dutyTeam;

}
