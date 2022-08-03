package com.timevale.forward.facade.api.request;

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
@ApiModel("项目节点流程新增")
public class ProjectNodeFlowAddReq extends BaseReq {

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("业务方")
    private List<PersonAddReq> bis;

    @ApiModelProperty("产品经理")
    private PersonAddReq pd;

    @ApiModelProperty("po负责人")
    private PersonAddReq po;

    @ApiModelProperty("D层负责人")
    private PersonAddReq d;

    @ApiModelProperty("调整前发布时间")
    private Date publishDate;

    @ApiModelProperty("调整后发布时间")
    private Date changePublishDate;

    @ApiModelProperty("立项预期上线时间")
    private Date pjEstablishPublishDate;

    @ApiModelProperty("变更事由")
    private String reason;

    @ApiModelProperty("变更类型:0需求变更、1资源不足、2优先级降低、3外部依赖、9其他")
    private Integer changeType;

    @ApiModelProperty("变更类型为其他时,填写")
    private String otherReason;

}
