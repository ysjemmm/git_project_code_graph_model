package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点地图新增")
public class TrackMapAddReq extends BaseReq {

    @ApiModelProperty("名称")
    @NotBlank(message = "名称不能为空")
    private String name;

    @ApiModelProperty("菜单层级")
    @NotBlank(message = "菜单层级不能为空")
    private Integer level;

    @ApiModelProperty("上级菜单id")
    @NotBlank(message = "上级菜单id不能为空")
    private Long parentId;

}
