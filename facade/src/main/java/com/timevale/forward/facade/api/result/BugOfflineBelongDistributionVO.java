package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BugOfflineBelongDistributionVO extends ToString {

    @ApiModelProperty("bug所属端")
    private Integer belong;

    @ApiModelProperty("bug所属端-描述")
    private String belongName;

    @ApiModelProperty("该端问题总数")
    private Long count;

}
