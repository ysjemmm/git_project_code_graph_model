package com.timevale.forward.facade.api.result;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author jingchun
 * create on 8/1/2023
 **/
@Getter
@Setter
@ApiModel("项目完成校验返回")
@NoArgsConstructor
@AllArgsConstructor
public class ModifyProjectCheckVO {

    @ApiModelProperty("校验类型")
    private int type;

    @ApiModelProperty("是否强制校验")
    private boolean critical;

    @ApiModelProperty("校验未通过原因")
    private String msg;

}
