package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;


/**
 * @author by YangXu
 * @date 2023/05/06 11:14
 */
@Getter
@Setter
@ApiOperation("线上bug验收申请")
public class BugOnlineAcceptanceReq extends ToString {

    @NotNull(message = "id必填")
    @ApiModelProperty("bug id")
    private Long id;

    @NotNull(message = "验收结果必填")
    @ApiModelProperty("验收是否通过")
    private Boolean pass;
}
