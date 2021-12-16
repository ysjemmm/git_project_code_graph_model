package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 14:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("添加相关人员")
public class PersonAddReq extends BaseReq {

    @ApiModelProperty("人员姓名")
    private String userName;

    @ApiModelProperty("人员id")
    private String userId;
}
