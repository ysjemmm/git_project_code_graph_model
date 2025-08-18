package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("分组产品需求数量查询")
public class ProductDemandGroupList extends QueryBase {

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private Integer priority;

    @ApiModelProperty("业务域")
    private Long bizDomainId;

    @ApiModelProperty("产品线")
    private Long productLineId;

    @ApiModelProperty("子产品线id")
    private Long subProductLineId;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,-10已暂停,-20已作废")
    private Integer status;

    @ApiModelProperty("负责人")
    private String ownerId;

    @ApiModelProperty("其他负责人")
    private String notInOwnerIds;

    @ApiModelProperty("排期时间")
    private Date expectScheduleTime;

    @ApiModelProperty("标签ids")
    private List<Long> labelIds;

    @ApiModelProperty("其他标签ids")
    private String notInLabelIds;

    @ApiModelProperty("产品需求类型")
    private Integer type;
}
