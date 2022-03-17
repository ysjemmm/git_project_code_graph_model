package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Date 2022/3/17 10:27
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug获取对应显示字段")
public class BugOnlineGetFieldReq extends BaseReq {
    @ApiModelProperty("产品线id集合")
    @NotNull(message = "产品线id不能为空")
    private List<Long> productLineIdList;
}