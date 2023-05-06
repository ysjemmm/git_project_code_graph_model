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
@ApiOperation("线上bug转业务需求申请")
public class BugOnlineToBizApplyReq extends ToString {

    @NotNull(message = "id必填")
    @ApiModelProperty("bug id")
    private Long id;

    @ApiModelProperty("操作类型：1申请，2同意，3拒绝")
    private Integer operate;
}
