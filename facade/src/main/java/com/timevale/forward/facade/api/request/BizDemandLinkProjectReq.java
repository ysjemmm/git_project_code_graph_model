package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ApiModel("关联业务需求、项目请求")
@Accessors(chain = true)
public class BizDemandLinkProjectReq extends ToString {

    @NotEmpty(message = "业务需求id不能为空")
    @ApiModelProperty("业务需求id")
    private List<Long> bizDemandIds;

    @NotNull(message = "项目id必填")
    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("关联、取消关联类型")
    private int type;

}
