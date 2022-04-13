package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;


/**
 * 业务变更列表日志查询列表
 *
 * @author yangxu
 * @date 2022/04/12
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务变更列表日志查询")
public class BizChangeLogQueryList extends QueryBase {
    @ApiModelProperty("主体id")
    @NotNull(message = "主体id不能为空")
    private Long mainId;

    @ApiModelProperty("内容变更记录类型:2项目，3产品需求，4业务需求")
    @NotNull(message = "内容变更记录类型不能为空")
    private Integer type;
}