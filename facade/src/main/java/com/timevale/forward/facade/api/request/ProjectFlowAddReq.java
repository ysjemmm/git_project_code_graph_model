package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

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
}
