package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("产品需求状态信息")
public class ProductDemandStatusVO extends ToString {

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废")
    private Integer status;

    @ApiModelProperty("状态名称")
    private String statusText;

}
