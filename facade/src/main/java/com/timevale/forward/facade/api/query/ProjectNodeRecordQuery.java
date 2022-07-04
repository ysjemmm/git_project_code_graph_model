package com.timevale.forward.facade.api.query;

import com.timevale.forward.facade.api.request.BaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点版本查询")
public class ProjectNodeRecordQuery extends BaseReq {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty("小版本号id")
    @NotNull(message = "小版本号id不能为空")
    private Long minId;

    @ApiModelProperty("大版本号id")
    @NotNull(message = "大版本号id不能为空")
    private Long maxId;

}
