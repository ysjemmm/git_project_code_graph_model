package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2022/02/21 15:19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页查询条件")
public class HomePageBaseReq extends BaseReq {

    @ApiModelProperty("tab类型：0为个人，1为团队")
    @NotNull(message = "tab类型不能为空")
    Integer tabType;

    @ApiModelProperty("用户类型：0为产品，1为开发，2为测试，3为经营管理")
    @NotNull(message = "用户类型不能为空")
    Integer userType;
}
