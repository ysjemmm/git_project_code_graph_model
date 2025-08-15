package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

/**
 * 视图分组条件VO
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel("视图分组条件")
public class ViewsGroupVO extends ToString {

    @ApiModelProperty("字段key")
    private String key;

    @ApiModelProperty("字段类型 0：基础字段，1：标签分类")
    private Integer type;

    @ApiModelProperty("字段名称")
    private String name;
}