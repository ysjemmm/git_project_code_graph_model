package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;


/**
 * @author by YangXu
 * @date 2023/03/10 19:44
 */
@Getter
@Setter
@ApiModel("历史记录对比请求")
public class HistoryRecordCmpReq extends ToString {

    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    Long id;

    @ApiModelProperty("小版本号")
    @NotNull(message = "小版本号不能为空")
    private BigDecimal minVersion;

    @ApiModelProperty("大版本号")
    @NotNull(message = "大版本号不能为空")
    private BigDecimal maxVersion;
}
