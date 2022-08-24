package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("发起验收")
public class ProjectAcceptanceAddReq extends BaseReq {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty("验收人员")
    @NotNull(message = "验收人员不能为空")
    @Valid
    @Size(max = 20,message = "验收人员数量不能超过20")
    private List<PersonAddReq> acceptors;

}
