package com.timevale.forward.dal.entity;

import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 故障工单(TroubleTicketDO)实体类
 *
 * @author yangxu
 * @since 2022-03-16 18:00:50
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TroubleTicketDO extends BaseDO {
    /**
     * 故障概述
     */
    private String name;
    
    /**
     * 故障影响
     */
    private String influence;
    
    /**
     * 故障类型 0 业务故障，1 数据故障
     */
    private Integer type;
    
    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 故障发生时间
     */
    private Date occurrenceTime;
    
    /**
     * 影响客户数（家）
     */
    private Integer influenceCount;
    
    /**
     * 受影响客户
     */
    private String influenceClient;
    
    /**
     * 故障持续时长, 0 ≤5分钟，1 5~20分钟，2 20~30分钟，3 30~60分钟，4 ＞60分钟
     */
    private Integer duringTime;
    
    /**
     * 不可用时长（分钟）
     */
    private Double failureTime;
    
    /**
     * 故障影响面分布 0 全网，1 单业务域，2 跨多个业务域
     */
    private Integer influenceScope;
    
    /**
     * 故障原因分布,0 功能问题，1 性能问题，2 数据问题，3 环境问题，4 安全问题，5 外部问题
     */
    private Integer reason;
    
    /**
     * 业务恢复时间
     */
    private Date restoreTime;
    
    /**
     * 复盘时间
     */
    private Date replayTime;
    
    /**
     * 是否有资损
     */
    private Boolean assetLoss;
    
    /**
     * 损失金额(元)
     */
    private Double amountLoss;
    
    /**
     * 时间线
     */
    private String timeLine;
    
    /**
     * 原因分析
     */
    private String reasonAnalysis;
    
    /**
     * 故障定级，0 P0， 10 P1，20 P2， -10 未达到级别
     */
    private Integer troubleRank;
    
    /**
     * 首要负责人
     */
    private String primePrincipal;
    
    /**
     * 首要负责人id
     */
    private String primePrincipalId;
    
    /**
     * 次要负责人
     */
    private String minorPrincipal;
    
    /**
     * 次要负责人id
     */
    private String minorPrincipalId;
    
    /**
     * 负责团队
     */
    private Long dutyTeam;

    /**
     * 是否监控发现
     */
    private Integer isMonitorDetect;
}

