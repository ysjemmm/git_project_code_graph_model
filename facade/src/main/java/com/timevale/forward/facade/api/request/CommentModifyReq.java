package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("评论修改")
public class CommentModifyReq extends BaseReq {

    @ApiModelProperty("评论id")
    @NotNull(message = "评论id不能为空")
    private Long id;

    @ApiModelProperty("附件列表")
    private List<FileAddReq> fileList;
}
