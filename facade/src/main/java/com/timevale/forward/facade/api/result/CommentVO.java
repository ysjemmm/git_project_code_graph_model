package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;


/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("评论列表")
public class CommentVO extends ToString {
    @ApiModelProperty("内容")
    private String content;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;



}
