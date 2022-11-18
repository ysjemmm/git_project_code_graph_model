package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/27 18:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求通知接收人")
public class BizDemandNoticeReceiverReq extends BaseReq {

    @ApiModelProperty("业务需求id")
    @NotNull(message = "业务需求id不能为空")
    private List<Long> bizDemandIds;
}
