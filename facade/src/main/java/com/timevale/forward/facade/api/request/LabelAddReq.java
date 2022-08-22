package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("标签新增")
public class LabelAddReq extends BaseReq {


    @ApiModelProperty("标签名称")
    @NotNull(message = "标签名称不能为空")
    private List<String> names;


    @ApiModelProperty("类别id")
    @NotNull(message = "类别id不能为空")
    private Long categoryId;


}
