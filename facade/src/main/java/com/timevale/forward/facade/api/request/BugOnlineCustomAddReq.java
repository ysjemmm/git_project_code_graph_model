package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yexuan
 * @date 2022-09-23 14:52 yexuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求新增")
public class BugOnlineCustomAddReq extends BaseReq{

    @ApiModelProperty("客户名称")
    private String customName;

    @ApiModelProperty("客户id")
    private Long customId;
}
