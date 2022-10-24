package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/16 14:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("故障工单-修改")
public class TroubleTicketModifyReq extends BaseReq {

    @NotNull(message = "故障工单id不能为空")
    @ApiModelProperty("故障工单id")
    private Long id;

    @ApiModelProperty("故障概述")
    @NotNull(message = "故障概述不能为空")
    private String name;

    @ApiModelProperty("故障影响")
    @NotNull(message = "故障影响不能为空")
    private String influence;

    @ApiModelProperty("故障类型 0 业务故障，1 数据故障")
    @NotNull(message = "故障类型不能为空")
    private Integer type;

    @ApiModelProperty("处理人")
    private List<PersonAddReq> handlerList;

    @ApiModelProperty("产品线id")
    @NotNull(message = "产品线不能为空")
    private List<Long> productLineIds;

    @ApiModelProperty("影响客户数（家）")
    private Integer influenceCount;

    @ApiModelProperty("受影响客户")
    @NotNull(message = "受影响客户不能为空")
    private String influenceClient;

    @NotNull(message = "故障发生时间不能为空")
    @ApiModelProperty("故障发生时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date occurrenceTime;

    @ApiModelProperty("业务恢复时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
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
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date replayTime;

    @ApiModelProperty("是否有资损")
    private Boolean assetLoss;

    @ApiModelProperty("损失金额（元）")
    private BigDecimal amountLoss;

    @ApiModelProperty("时间线")
    @NotNull(message = "时间线不能为空")
    private String timeLine;

    @ApiModelProperty("原因分析")
    @NotNull(message = "原因分析不能为空")
    private String reasonAnalysis;

    @ApiModelProperty("故障定级 0 P0， 10 P1，20 P2， -10 未达到级别")
    private Integer troubleRank;

    @ApiModelProperty("主责任人")
    @NotNull(message = "主责任人不能为空")
    private String primePrincipal;

    @ApiModelProperty("主责任人id")
    @NotNull(message = "主责任人id不能为空")
    private String primePrincipalId;

    @ApiModelProperty("次责任人")
    @NotNull(message = "次责任人不能为空")
    private String minorPrincipal;

    @ApiModelProperty("次责任人id")
    @NotNull(message = "次责任人id不能为空")
    private String minorPrincipalId;

    @ApiModelProperty("责任团队id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dutyTeam;

    @ApiModelProperty("是否监控发现")
    @NotNull(message = "是否监控发现不能为空")
    private Integer isMonitorDetect;

    @ApiModelProperty("附件列表")
    private List<FileAddReq> fileList;

    @ApiModelProperty("故障持续时长")
    private BigDecimal durationTime;

    @ApiModelProperty("故障上报时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date reportTime;

    @ApiModelProperty("故障响应时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date responseTime;

    @ApiModelProperty("定位到问题原因时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date locationTime;

    @ApiModelProperty("故障解决时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date solveTime;

    @ApiModelProperty("原因类型：0代码导致，10其它原因")
    private Integer cause;

    @ApiModelProperty("故障扣分")
    private Long deductPoints;

    @ApiModelProperty("数据统计说明")
    @NotNull(message = "数据统计说明不能为空")
    private String dataStatistics;
}
