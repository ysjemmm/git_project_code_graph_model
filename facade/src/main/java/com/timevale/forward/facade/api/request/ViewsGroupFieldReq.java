package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@Data
@ApiModel("视图分组字段")
public class ViewsGroupFieldReq {
    @ApiModelProperty(value = "字段key", required = true)
    private String key;

    @ApiModelProperty(value = "字段类型 0：基础字段，1：标签分类", required = true)
    private Integer type;

    @ApiModelProperty(value = "字段名称", required = true)
    private String name;
}
