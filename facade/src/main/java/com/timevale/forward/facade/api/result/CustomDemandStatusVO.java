package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by xingyun
 * @date 2022/01/04 13:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("客户需求状态信息")
public class CustomDemandStatusVO extends ToString {

    @ApiModelProperty("需求解决状态:0待评估,10已接收,15已完成无需开发,17已关联产品需求,20已列入项目,30项目进行中,40已完成上线,-10被驳回")
    private Integer status;

    @ApiModelProperty("需求解决状态名称")
    private String statusText;

    @ApiModelProperty("项目发布时间")
    private Date projectEndDate;
}
