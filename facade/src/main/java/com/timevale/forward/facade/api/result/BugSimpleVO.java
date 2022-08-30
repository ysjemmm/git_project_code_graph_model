package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Date 2022/2/24 10:49
 * @Author 望轩
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("bug数据")
public class BugSimpleVO extends ToString {

    @ApiModelProperty(value = "bugId")
    private Long id;

    @ApiModelProperty("bug名称")
    private String name;
}