package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xiaoyun
 * @date 2022/9/1/001 14:18
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务标签简单信息")
public class BizLabelSimpleVO extends ToString {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("类别id")
    private Long labelCategoryId;

    @ApiModelProperty("类别名称")
    private String labelCategoryName;
}
