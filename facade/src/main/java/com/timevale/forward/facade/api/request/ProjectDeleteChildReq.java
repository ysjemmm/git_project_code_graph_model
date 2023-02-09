package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author jingchun
 * created on 2023/2/9
 */
@Getter
@Setter
public class ProjectDeleteChildReq extends ToString {

    @NotNull(message = "父项目id必填")
    @ApiModelProperty("父项目id")
    private Long projectId;

    @NotNull(message = "子项目id必填")
    @ApiModelProperty("子项目id")
    private Long childId;

}
