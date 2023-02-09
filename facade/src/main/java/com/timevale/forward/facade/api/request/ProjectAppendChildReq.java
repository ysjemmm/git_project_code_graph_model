package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/8
 */
@Getter
@Setter
@ApiModel("添加子项目列表入参")
public class ProjectAppendChildReq extends ToString {

    @NotNull(message = "父项目id必填")
    @ApiModelProperty("父项目id")
    private Long projectId;

    @NotEmpty(message = "子项目id列表不能为空")
    @ApiModelProperty("子项目id列表")
    private List<Long> childIds;

}
