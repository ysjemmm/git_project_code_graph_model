package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("分组产品需求数量查询")
public class BizDemandGroupList extends QueryBase {

    @ApiModelProperty("优先级:0(P0),10(P1),20(P2),30(P3)")
    private Integer priority;

    @ApiModelProperty("业务域")
    private Long bizDomainId;

    @ApiModelProperty("产品线")
    private Long productLineId;

    @ApiModelProperty("子产品线id")
    private Long subProductLineId;

    @ApiModelProperty("需求解决状态:0待评估，10已接收，13待确认，15已完成无需开发，17已关联产品需求，20已列入项目，30项目进行中，40已完成上线，-10被驳回，-20已作废")
    private Integer status;

    @ApiModelProperty("需求接收人")
    private String receiveManId;

    @ApiModelProperty("其他需求接收人")
    private String notInReceiveManIds;

    @ApiModelProperty("需求部门")
    private Long deptId;

    @ApiModelProperty("标签ids")
    private List<Long> labelIds;

    @ApiModelProperty("其他标签ids")
    private String notInLabelIds;

    @ApiModelProperty("目标客户")
    private String targetCustomer;

    @ApiModelProperty("客户等级")
    private String customerGrade;
}
