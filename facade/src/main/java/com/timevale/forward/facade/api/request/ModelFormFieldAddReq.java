package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * @auther: yuhua
 * @date: 2025/11/3 14:51
 * @description:
 */
@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("模块表单字段")
@Accessors(chain = true)
public class ModelFormFieldAddReq extends ToString {

    @ApiModelProperty(value = "模块id", required = true)
    private String modelId;

    @ApiModelProperty(value = "字段名称", required = true)
    @NotBlank(message = "字段名称不能为空")
    private String fieldName;

    @ApiModelProperty(value = "字段", required = true)
    @NotBlank(message = "字段不能为空")
    private String field;

    @ApiModelProperty(value = "字段值", required = true)
    private String fieldValue;
}
