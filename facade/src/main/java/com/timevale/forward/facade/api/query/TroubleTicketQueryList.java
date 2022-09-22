package com.timevale.forward.facade.api.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
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
    private List<Integer> troubleRankList;

    @ApiModelProperty("业务域id-列表")
    private List<Long> bizDomainIdList;

    @ApiModelProperty("产品线id-列表")
    private List<Long> productLineIdList;

    @ApiModelProperty("故障发生时间-起始")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date occurrenceTimeStart;

    @ApiModelProperty("故障发生时间-结束")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date occurrenceTimeEnd;

    @ApiModelProperty("主责任人id-列表")
    private List<String> primePrincipalIdList;

    @ApiModelProperty("责任团队id-列表")
    private List<Long> dutyTeamList;

    @ApiModelProperty("是否监控发现 0否， 1是")
    private Integer isMonitorDetect;

    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;

    @ApiModelProperty("排序字段")
    private String orderFiled;

    @ApiModelProperty("排序规则：0正序，1逆序")
    private Integer orderCollation;

    @ApiModelProperty("改进措施未完成")
    @NotNull(message = "改进措施未完成不能为空")
    private Boolean disComplete;
}
