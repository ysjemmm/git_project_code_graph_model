package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点流程详情")
public class ProjectNodeFlowDetailVO extends ToString {

    @ApiModelProperty("状态:-1撤回,0审核中,1审核通过,2审核不通过")
    private Integer status;

    @ApiModelProperty("状态")
    private String statusName;

    @ApiModelProperty("变更事由")
    private String reason;

    @ApiModelProperty("发起人")
    private String createMan;

    @ApiModelProperty("产品经理")
    private String pd;

    @ApiModelProperty("业务方")
    private List<String> bis;

    @ApiModelProperty("po负责人")
    private String po;

    @ApiModelProperty("D层负责人")
    private String d;

    @ApiModelProperty("变更通过次数")
    private Long changeCount;

    @ApiModelProperty("延迟(工作日)")
    private BigDecimal delayDay;

    @ApiModelProperty("未评审人员")
    private List<String> unrevieweds;

    @ApiModelProperty("评审不通过人员")
    private List<String> reviewFails;

    @ApiModelProperty("未通过原因")
    private String reviewFailReason;

    @ApiModelProperty("PO/D审批不通过原因")
    private String poReviewFailReason;

    @ApiModelProperty("调整前发布时间")
    private Date publishDate;

    @ApiModelProperty("调整后发布时间")
    private Date changePublishDate;

    @ApiModelProperty("立项预期上线时间")
    private Date pjEstablishPublishDate;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("变更类型:0需求变更、1资源不足、2优先级降低、3外部依赖、9其他")
    private Integer changeType;

    @ApiModelProperty("变更类型为其他时,填写")
    private String otherReason;
}
