package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import io.swagger.annotations.ApiModelProperty;
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
    @WildcardEscape
    private String name;

    /**
     * 故障单id-列表
     */
    private List<Long> troubleTicketIdList;

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
    private List<Integer> troubleRankList;

    /**
     * 故障定级-是否可以为空
     */
    private Boolean troubleRankIsNull;

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

    /**
     * 主责任人id-列表
     */
    private List<String> primePrincipalIdList;

    /**
     * 责任团队id-列表
     */
    private List<Long> dutyTeamList;

    /**
     * 是否监控发现 0否， 1是
     */
    private Boolean isMonitorDetect;
}
