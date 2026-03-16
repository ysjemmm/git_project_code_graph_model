package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 线下Bug分组筛选条件（父分组筛选）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下Bug分组筛选条件")
public class BugOfflineGroupList extends QueryBase {

    @ApiModelProperty("优先级:0(P0),10(P1),20(P2),30(P3)")
    private Integer priority;

    @ApiModelProperty("业务域")
    private Long bizDomainId;

    @ApiModelProperty("产品线")
    private Long productLineId;

    @ApiModelProperty("Bug状态")
    private Integer status;

    @ApiModelProperty("严重程度")
    private Integer severity;

    @ApiModelProperty("经办人")
    private String operatorId;

    @ApiModelProperty("其他经办人")
    private String notInOperatorIds;

    @ApiModelProperty("提出人")
    private String proposerId;

    @ApiModelProperty("其他提出人")
    private String notInProposerIds;

    @ApiModelProperty("标签ids")
    private List<Long> labelIds;

    @ApiModelProperty("其他标签ids")
    private String notInLabelIds;
}
