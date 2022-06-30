package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/06/30 19:31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求-产品线-信息")
public class BizDemandProductLineVO extends ToString {

    @ApiModelProperty("产品线名称")
    private String name;

    @ApiModelProperty("数量")
    private Integer count;
}
