package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BugOfflineReasonDistributionVO extends ToString {

    @ApiModelProperty(value = "bug原因")
    private Integer reason;

    @ApiModelProperty("bug原因-描述")
    private String reasonName;

    @ApiModelProperty("该原因问题总数")
    private Long count;

}
