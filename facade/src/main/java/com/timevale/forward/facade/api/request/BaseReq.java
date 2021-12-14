package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author yuankai
 * @date 2021/10/25 18:49
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class BaseReq extends ToString {
    @ApiModelProperty(value = "操作人花名拼音", hidden = true)
    private String account;

    @ApiModelProperty(value = "操作人花名", hidden = true)
    private String alias;
}
