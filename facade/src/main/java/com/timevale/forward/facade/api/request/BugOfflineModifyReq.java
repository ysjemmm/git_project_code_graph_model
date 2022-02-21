package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug编辑")
public class BugOfflineModifyReq extends BugOfflineAddReq {

    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    private Long id;

}
