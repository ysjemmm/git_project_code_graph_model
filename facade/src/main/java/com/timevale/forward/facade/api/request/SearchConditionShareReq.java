package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 查询条件分享请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("查询条件分享请求")
public class SearchConditionShareReq extends BaseReq {

    @ApiModelProperty("查询条件id")
    @NotNull(message = "id不能为空")
    private Long id;

    @ApiModelProperty("分享目标用户id列表")
    @NotEmpty(message = "分享人不能为空")
    private List<String> userIds;
}
