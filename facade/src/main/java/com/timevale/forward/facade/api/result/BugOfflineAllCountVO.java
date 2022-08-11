package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BugOfflineAllCountVO extends ToString {

    @ApiModelProperty("待修复数量")
    private Long waitRepairCount;

    @ApiModelProperty("高优先级待修复数量")
    private Long urgentRepairCount;

}
