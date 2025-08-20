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
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("评论列表")
public class CommentVO extends ToString {
    @ApiModelProperty("评论id")
    private Long id;

    @ApiModelProperty("内容")
    private String content;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建人Id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("附件")
    private List<FileVO> fileList;

    @ApiModelProperty("是否被删除")
    private Boolean isDeleted;

    @ApiModelProperty("修改时间")
    private Date modifyDate;

    @ApiModelProperty("是否被修改")
    private Boolean isModify = false;
}
