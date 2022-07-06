package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jingchun
 * create on 2022/7/1
 */
@Getter
@Setter
public class ProductDemandDescFlowVO extends ToString {

    @ApiModelProperty("产品需求id")
    private Long productDemandId;

    @ApiModelProperty("流程id")
    private String flowId;

    @ApiModelProperty("任务id")
    private String taskId;

    @ApiModelProperty("评审状态:-1已撤回,0审核中,1审核通过,2审核不通过")
    private Integer status;

    @ApiModelProperty("流程阶段:0:第一阶段,1:第二阶段")
    private Integer stage;

    @ApiModelProperty("变更类型:0需求调研不充分、1业务需求变更或新增、2业务需求理解偏差、3需求对现有业务流造成改动需调整方案、9其他")
    private Integer changeType;

    @ApiModelProperty("变更原因")
    private String reason;

    @ApiModelProperty("评审不通过原因")
    private String reviewFailReason;

    @ApiModelProperty("项目经理id")
    private String pmId;

    @ApiModelProperty("项目经理")
    private String pm;

    @ApiModelProperty("项目经理id")
    private String poId;

    @ApiModelProperty("项目经理")
    private String po;
}
