package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("业务需求变更日志content详情")
public class BizChangeLogContentVO extends ToString {

    @ApiModelProperty("低代码任务id")
    private String taskId;

}
