package com.timevale.forward.facade.api.request;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/03/16 14:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("故障工单-催办")
public class TroubleTicketRemindReq extends BaseReq {
    
    @ApiModelProperty("故障工单ids")
    @NotNull(message = "故障工单id不能为空")
    private List<Long> ids;

    }
