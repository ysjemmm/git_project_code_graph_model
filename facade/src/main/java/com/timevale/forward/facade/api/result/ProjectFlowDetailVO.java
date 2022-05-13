package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
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
@ApiModel("项目流程详情")
public class ProjectFlowDetailVO extends ToString {

    @ApiModelProperty("状态:-1撤回,0审核中,1审核通过,2审核不通过")
    private Integer status;

    @ApiModelProperty("状态")
    private String statusName;

    @ApiModelProperty("发起人")
    private PersonVO proposerVO;

    @ApiModelProperty("会议时间")
    private Date reviewDate;

    @ApiModelProperty("评审人员")
    private List<PersonVO> reviews;

    @ApiModelProperty("评审通过人员")
    private List<String> revieweds;

    @ApiModelProperty("未评审人员")
    private List<String> unrevieweds;

    @ApiModelProperty("评审不通过人员")
    private List<String> reviewFails;

    @ApiModelProperty("未通过原因")
    private String reviewFailReason;

    @ApiModelProperty("详设地址")
    private String reviewUrl;

    @ApiModelProperty("附件")
    private List<FileVO> files;

    @ApiModelProperty("打回次数")
    private Long returnCount;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;
    
}
