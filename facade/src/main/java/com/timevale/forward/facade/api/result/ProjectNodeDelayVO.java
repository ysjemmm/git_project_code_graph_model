package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点流程延期")
public class ProjectNodeDelayVO extends ToString {

    @ApiModelProperty("延期天数")
    private BigDecimal delayDay;

    @ApiModelProperty("延期类型:-1不延期,0提测延期,1发布正式延期,2立项预期上线时间小于发布正式计划时间")
    private Integer delayType;

}
