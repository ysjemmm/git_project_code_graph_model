package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/06/28 10:52
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("更新时间")
public class UpdateTimeVO extends ToString {

    @ApiModelProperty("更新时间")
    private Date updateTime;
}
