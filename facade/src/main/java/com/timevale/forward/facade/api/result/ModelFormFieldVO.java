package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @auther: yuhua
 * @date: 2025/11/3 14:24
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("模块表单字段信息")
public class ModelFormFieldVO extends ToString {

    @ApiModelProperty("modelId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long modelId;

    @ApiModelProperty("表单字段名称")
    private String fieldName;

    @ApiModelProperty("表单字段")
    private String field;

    @ApiModelProperty("表单字段值")
    private String fieldValue;
}
