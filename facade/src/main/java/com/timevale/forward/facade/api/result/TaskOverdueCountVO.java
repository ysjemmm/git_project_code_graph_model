package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskOverdueCountVO extends ToString {

    @ApiModelProperty("人员id")
    private String userId;

    @ApiModelProperty("人员名字")
    private String userName;

    @ApiModelProperty("逾期任务数量")
    private Long overdueCount;

    @ApiModelProperty("累计逾期时长(毫秒)")
    private Long accumulateOverdueMillis;

}
