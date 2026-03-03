package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量延期修复线下bug请求
 * 
 * @author system
 * @date 2026-03-03
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("批量延期修复线下bug")
public class BugOfflineBatchDelayReq extends BaseReq {

    @ApiModelProperty("bug ID列表")
    @NotEmpty(message = "bug ID列表不能为空")
    private List<Long> bugIds;

    @ApiModelProperty("延期修复原因")
    private String delayHandleReason;
}
