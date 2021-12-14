package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author: xingyun
 * @create: 2021-12-14 11:32
 **/
@Data
@ApiModel("人员信息")
public class UserInfo {

    @ApiModelProperty("拼音")
    private String userId;

    @ApiModelProperty("名称")
    private String userName;
}
