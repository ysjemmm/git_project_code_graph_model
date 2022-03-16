package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 故障工单(TroubleTicketDO)实体类
 *
 * @author yangxu
 * @since 2022-03-16 18:00:50
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TroubleTicketListDO extends BaseDO {
    /**
     * 故障概述
     */
    private String name;

    /**
     * 故障定级，0 P0， 10 P1，20 P2， -10 未达到级别
     */
    private Integer troubleRank;

    /**
     * 处理人
     */
    private String handler;

    /**
     * 处理人id
     */
    private String handlerId;

    /**
     * 业务域
     */
    private String bizDomainName;

    /**
     * 产品线
     */
    private String productLineName;
}

