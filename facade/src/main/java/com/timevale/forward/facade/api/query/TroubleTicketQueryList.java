package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/16 16:05
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("故障单-列表查询")
public class TroubleTicketQueryList extends QueryBase {

    @ApiModelProperty("故障概述")
    private String name;

    @ApiModelProperty("处理人id-列表")
    private List<String> handlerIdList;

    @ApiModelProperty("提出人id-列表")
    private List<String> createMandIdList;

    @ApiModelProperty("故障定级-列表")
    private List<Integer> troubleRank;

    @ApiModelProperty("业务域id-列表")
    private List<Long> bizDomainIdList;

    @ApiModelProperty("产品线id-列表")
    private List<Long> productLineIdList;

    @ApiModelProperty("故障发生时间-起始")
    private Date occurrenceTimeStart;

    @ApiModelProperty("故障发生时间-结束")
    private Date occurrenceTimeEnd;

}
