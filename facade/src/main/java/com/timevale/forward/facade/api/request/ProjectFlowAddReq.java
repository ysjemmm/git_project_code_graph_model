package com.timevale.forward.facade.api.request;

import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目流程新增")
public class ProjectFlowAddReq extends BaseReq {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty("发起人")
    @NotNull(message = "发起人不能为空")
    private PersonAddReq proposer;

    @ApiModelProperty("会议时间")
    @NotNull(message = "会议时间不能为空")
    private Date reviewDate;

    @ApiModelProperty("评审人员")
    @NotNull(message = "评审人员不能为空")
    private List<PersonAddReq> reviews;

    @ApiModelProperty("详设地址")
    private String reviewUrl;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;

    @ApiModelProperty("流程类型：DEMAND_INTERNAL_AUDIT：需求内审;DEMAND_CONSTRUE:需求串讲;UED_AUDIT:ued评审;TECHNICAL_DETAIL_REVIEW:详设评审")
    @NotBlank(message = "流程类型不能为空")
    private String flowType;
}
