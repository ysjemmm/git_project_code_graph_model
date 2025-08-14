package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图分享请求")
public class ViewsShareReq extends ViewsReq {
    @ApiModelProperty(value = "分享人列表", required = true)
    @NotNull(message = "分享人列表不能为空")
    private List<PersonAddReq> users;
}
