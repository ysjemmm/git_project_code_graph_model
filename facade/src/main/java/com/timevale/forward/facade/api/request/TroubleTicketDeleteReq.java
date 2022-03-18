package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2022/03/16 14:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("故障工单-删除")
public class TroubleTicketDeleteReq extends BaseReq {

    @ApiModelProperty("故障工单id")
    @NotNull(message = "故障工单id不能为空")
    private Long id;

}
