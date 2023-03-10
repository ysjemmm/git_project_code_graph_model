package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


/**
 * @author by YangXu
 * @date 2023/03/10 19:35
 */
@Getter
@Setter
@ApiModel("历史记录对比")
public class HistoryRecordCmpVO extends ToString {

    @ApiModelProperty("用户名称")
    private String userName;

    @ApiModelProperty("小版本")
    private BigDecimal minVersion;

    @ApiModelProperty("大版本")
    private BigDecimal maxVersion;

    @ApiModelProperty("时差(工作日)天")
    private BigDecimal timeDiff;
}
