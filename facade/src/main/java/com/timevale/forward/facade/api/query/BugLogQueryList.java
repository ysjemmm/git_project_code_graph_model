package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/2/25 16:45
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("bug日志列表查询")
public class BugLogQueryList extends QueryBase {
    @ApiModelProperty("bug的id")
    @NotNull(message = "bug的id不能为空")
    private Long id;

    @ApiModelProperty("内容变更记录类型:0线下bug,1线上bug")
    @NotNull(message = "内容变更记录类型不能为空")
    private Integer type;
}