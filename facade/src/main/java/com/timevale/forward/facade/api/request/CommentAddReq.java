package com.timevale.forward.facade.api.request;

import com.timevale.forward.facade.api.query.PersonQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    private Long toId;

    @ApiModelProperty("主体类型:0:项目,1产品需求,2业务需求")
    private Byte type;

    @ApiModelProperty("内容")
    private String content;

    @ApiModelProperty("评论接收人")
    private List<PersonQuery> receiverInfoList;

}
