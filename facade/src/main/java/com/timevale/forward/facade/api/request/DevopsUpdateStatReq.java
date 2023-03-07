package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2023/02/24 16:30
 */
@Getter
@Setter
@ApiModel("发布平台应用更新勾选状态")
public class DevopsUpdateStatReq extends ToString {

    @ApiModelProperty(value = "id")
    @NotNull(message = "id必填")
    private Long id;

    @ApiModelProperty(value = "勾选状态")
    @NotNull(message = "勾选状态必填")
    private Boolean statFlag;
}
