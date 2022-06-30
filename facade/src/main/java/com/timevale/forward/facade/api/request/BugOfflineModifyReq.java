package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug编辑")
public class BugOfflineModifyReq extends BugOfflineAddReq {

    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    private Long id;

    @ApiModelProperty("延期修复原因")
    private String delayHandleReason;

    @ApiModelProperty("不用修复原因")
    private Integer unhandleReason;

    @ApiModelProperty("bug产生原因")
    private String cause;

    @ApiModelProperty("解决方案")
    private String solvePlan;

    @ApiModelProperty("预计解决完成日期")
    private Date expectSolveDate;
}
