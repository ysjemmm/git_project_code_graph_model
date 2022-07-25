package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BugOfflineCountVO extends ToString {

    @ApiModelProperty("人员id")
    private String userId;

    @ApiModelProperty("人员名字")
    private String userName;

    @ApiModelProperty("待修复数量")
    private Long waitRepairCount;

    @ApiModelProperty("高优先级待修复数量")
    private Long urgentRepairCount;

}
