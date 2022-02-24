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
@ApiModel("抄送人")
public class RecipientAddReq extends BaseReq {

    @ApiModelProperty(value = "抄送人")
    @NotNull(message = "抄送人不能为空")
    private List<PersonAddReq> recipients;

    @ApiModelProperty(value = "产品需求或业务需求或线下bug的id")
    @NotNull(message = "主体id不能为空")
    private Long mainId;

    @ApiModelProperty(value = "项目-产品经理,1项目-项目成员,20产品需求-抄送人,30业务需求-抄送人,40-任务执行人,50线下bug-抄送人")
    @NotNull(message = "类型不能为空")
    private Integer type;


}
