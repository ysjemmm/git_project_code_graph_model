package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

/**
 * @author yexuan
 * @date 2022-09-23 14:52 yexuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug群新增")
public class BugOnlineGroupAddReq extends BaseReq{

    @ApiModelProperty("群名称")
    @Length(max = 200, message = "群名称长度不能超过200")
    private String groupName;

    @ApiModelProperty("群id")
    @Length(max = 50, message = "群id长度不能超过50")
    private String groupId;
}
