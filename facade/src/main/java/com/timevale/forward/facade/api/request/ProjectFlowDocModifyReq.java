package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ApiModel("产品流程文档维护对象")
public class ProjectFlowDocModifyReq extends ToString {

    @NotNull(message = "项目id必填")
    @ApiModelProperty("项目id")
    private Long projectId;

    @NotNull(message = "流程类型必填")
    @ApiModelProperty("流程类型:10-需求内审;20-需求串讲;27-UED评审;30-详设评审")
    private Integer flowType;

    @ApiModelProperty("流程关联地址")
    private String reviewUrl;

    @Valid
    @ApiModelProperty("附件列表")
    private List<FileAddReq> files;

}
