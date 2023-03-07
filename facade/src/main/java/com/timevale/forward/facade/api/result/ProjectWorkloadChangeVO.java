package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


/**
 * @author by YangXu
 * @date 2023/03/07 11:08
 */
@Getter
@Setter
@ApiModel("工作量变更申请详情")
public class ProjectWorkloadChangeVO extends ToString {

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目类型")
    private String kindName;

    @ApiModelProperty("项目性质")
    private String typeName;

    @ApiModelProperty("项目等级")
    private String levelName;

    @ApiModelProperty("变更类型")
    private String changeType;

    @ApiModelProperty("计划工作量—调整前")
    private BigDecimal planWorkloadBefore;

    @ApiModelProperty("计划工作量—调整后")
    private BigDecimal planWorkloadAfter;

    @ApiModelProperty("计划工作量增加")
    private BigDecimal planWorkloadAddSum;

    @ApiModelProperty("积分工作量—调整前")
    private BigDecimal pointWorkloadBefore;

    @ApiModelProperty("积分工作量—调整后")
    private BigDecimal pointWorkloadAfter;

    @ApiModelProperty("积分工作量增加")
    private BigDecimal pointWorkloadAddSum;
}
