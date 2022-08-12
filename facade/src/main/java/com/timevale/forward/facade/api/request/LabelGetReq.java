package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("标签详情")
public class LabelGetReq extends BaseReq {


    @ApiModelProperty("标签名称")
    private Long labelId;


    @ApiModelProperty("类别id")
    private Long categoryId;


}
