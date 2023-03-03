package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("项目成员评分详情")
public class ProjectMemberEvaluateVO extends ToString {

    @ApiModelProperty("工作量变更审批流程状态")
    private Integer workloadFlowIdStatus;

    @ApiModelProperty("工作量变更审批流程id")
    private String workloadFlowId;

    @ApiModelProperty("结项工作流id")
    private String conclusionFlowId;

    @ApiModelProperty("计划总工作量")
    private BigDecimal planWorkloadSum;

    @ApiModelProperty("工作量(计算积分)")
    private BigDecimal workloadPointsSum;

    @ApiModelProperty("项目成员评分")
    private List<MemberEvaluateVO> memberEvaluateVOList;
}
