package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;


/**
 * @author by YangXu
 * @date 2022/08/19 16:29
 */
@Data
@ApiModel("人天提报批量同意请求")
public class ManDayReportBatchApproveReq extends BaseReq {

    @NotEmpty(message = "提报id不能为空")
    @ApiModelProperty("提报id列表")
    private List<Long> ids;
}
