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

    @NotNull(message = "项目流程id必填")
    @ApiModelProperty("项目流程id")
    private Long id;

    @ApiModelProperty("流程关联地址")
    private String reviewUrl;

    @Valid
    @ApiModelProperty("附件列表")
    private List<FileAddReq> files;

}
