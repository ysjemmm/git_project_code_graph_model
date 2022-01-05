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

    @ApiModelProperty(value = "产品需求或业务需求id")
    @NotNull(message = "主体id不能为空")
    private Long mainId;


}
