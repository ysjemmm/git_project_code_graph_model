package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/3
 */
@Getter
@Setter
@ApiOperation("线上bug关联业务需求列表接口")
public class BugOnlineAttachToBizReq extends ToString {

    @NotNull(message = "线上bug id不能为空")
    @ApiModelProperty("bug id")
    private Long id;

    @NotEmpty(message = "业务需求id列表不能为空")
    private List<Long> bizDemandIds;

}
