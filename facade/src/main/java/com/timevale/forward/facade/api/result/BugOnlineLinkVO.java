package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Date 2022/3/17 10:39
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("关联线上bug")
public class BugOnlineLinkVO extends ToString {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("bug标题")
    private String name;

}