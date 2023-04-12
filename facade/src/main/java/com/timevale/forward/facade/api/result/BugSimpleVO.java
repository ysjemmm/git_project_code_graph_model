package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @Date 2022/2/24 10:49
 * @Author 望轩
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("bug数据")
public class BugSimpleVO extends ToString {

    @ApiModelProperty(value = "bugId")
    private Long id;

    @ApiModelProperty("bug名称")
    private String name;
}