package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/14 15:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("发布计划查询")
public class PublishPlanQueryList extends QueryBase {

    @ApiModelProperty("发布计划名称")
    private String name;

    @ApiModelProperty("发布计划id")
    private Long id;

    @ApiModelProperty("应用名称")
    private String appName;

    @ApiModelProperty("创建人花名中文")
    private String createMan;
}
