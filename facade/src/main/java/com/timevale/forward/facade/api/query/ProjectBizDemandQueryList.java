package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jingchun
 * created on 2023/4/18
 */
@Getter
@Setter
@ApiOperation("项目业务需求查询接口")
public class ProjectBizDemandQueryList extends QueryBase {

    @ApiModelProperty("项目id")
    private Long projectId;

}
