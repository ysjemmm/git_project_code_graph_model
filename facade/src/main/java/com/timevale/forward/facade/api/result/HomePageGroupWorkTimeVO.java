package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jingchun
 * created on 2023/2/3
 */
@Getter
@Setter
@ApiOperation("团队项目工时看板")
public class HomePageGroupWorkTimeVO extends ToString {

    @ApiModelProperty("用户账户")
    private String account;

    @ApiModelProperty("用户花名")
    private String alias;

}
