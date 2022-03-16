package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/16 18:22
 */
@Data
@Builder
public class TroubleTicketCondition {

    /**
     * 故障概述
     */
    private String name;

    /**
     * 处理人id-列表
     */
    private List<String> handlerIdList;

    /**
     * 提出人id-列表
     */
    private List<String> createMandIdList;

    /**
     * 故障定级-列表
     */
    private List<Integer> troubleRank;

    /**
     * 业务域id-列表
     */
    private List<Long> bizDomainIdList;

    /**
     * 产品线id-列表
     */
    private List<Long> productLineIdList;

    /**
     * 故障发生时间-起始
     */
    private Date occurrenceTimeStart;

    /**
     * 故障发生时间-结束
     */
    private Date occurrenceTimeEnd;
}
