package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/04/24 17:56
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目风险列表查询")
public class ProjectRiskQueryList extends QueryBase {

    @ApiModelProperty("项目id")
    private Long projectId;
}
