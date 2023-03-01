package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2023/02/24 16:27
 */
@Getter
@Setter
@ApiModel("发布平台项目数据")
public class DevopsProjectVO extends ToString {

    @ApiModelProperty(value = "发布平台项目名称")
    private String devopsProjectName;

    @ApiModelProperty(value = "发布平台项目标识")
    private String devopsProjectSign;
}
