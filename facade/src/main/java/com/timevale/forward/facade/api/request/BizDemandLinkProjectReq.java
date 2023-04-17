package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@ApiModel("关联业务需求、项目请求")
public class BizDemandLinkProjectReq extends ToString {

    @ApiModelProperty("业务需求id")
    private List<Long> bizDemandIds;

    @ApiModelProperty("项目id")
    private Long projectId;

}
