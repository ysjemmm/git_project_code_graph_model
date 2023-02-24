package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
@ApiModel("发布平台应用数据")
public class DevopsAppVO extends ToString {
    @ApiModelProperty(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty(value = "主体id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mainId;

    @ApiModelProperty(value = "发布平台项目名称")
    private String devopsProjectName;

    @ApiModelProperty(value = "发布平台项目标识")
    private String devopsProjectSign;

    @ApiModelProperty(value = "应用名称")
    private String appName;

    @ApiModelProperty(value = "应用类型")
    private String appType;

    @ApiModelProperty(value = "应用分支")
    private String appBranch;

    @ApiModelProperty(value = "域名")
    private String appDomain;

    @ApiModelProperty(value = "勾选标记")
    private Boolean statFlag;
}
