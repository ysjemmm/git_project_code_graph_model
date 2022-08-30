package com.timevale.forward.facade.api.request;

import com.timevale.forward.facade.api.query.PersonQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("评论新增")
public class CommentAddReq extends BaseReq {

    @ApiModelProperty("主体id")
    @NotNull(message = "主体id不能为空")
    private Long toId;

    @ApiModelProperty("主体类型:0:项目,1产品需求,2业务需求,3任务,4线下bug,5线上bug,6故障单,7客户需求")
    @NotNull(message = "主体类型不能为空")
    private Integer type;

    @ApiModelProperty("内容")
    @NotBlank(message = "评论内容不能为空")
    private String content;

    @ApiModelProperty("评论接收人")
    private List<PersonQuery> receiverInfoList;

    @ApiModelProperty("附件列表")
    private List<FileAddReq> fileList;
}
