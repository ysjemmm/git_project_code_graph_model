package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;


/**
 * @author by YangXu
 * @date 2023/03/10 19:35
 */
@Getter
@Setter
@ApiModel("历史记录对比")
public class HistoryRecordVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("版本")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal version;

    @ApiModelProperty("提交人")
    private String createMan;

    @ApiModelProperty("提交人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;
}
