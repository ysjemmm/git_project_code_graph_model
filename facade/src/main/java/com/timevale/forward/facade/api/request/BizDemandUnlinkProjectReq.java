package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("取消项目、业务需求关联关系请求")
public class BizDemandUnlinkProjectReq extends ToString {

    @ApiModelProperty("项目id")
    private String projectId;

    @ApiModelProperty("业务需求id")
    private String bizDemandId;

}
