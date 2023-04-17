package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ApiModel("取消项目、业务需求关联关系请求")
public class BizDemandUnlinkProjectReq extends ToString {

    @NotNull(message = "项目id必填")
    @ApiModelProperty("项目id")
    private Long projectId;

    @NotNull(message = "业务需求id必填")
    @ApiModelProperty("业务需求id")
    private Long bizDemandId;

}
