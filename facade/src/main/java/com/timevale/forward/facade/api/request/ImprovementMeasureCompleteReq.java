package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/03/16 15:21
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("改进措施-完成")
public class ImprovementMeasureCompleteReq extends BaseReq {

    @ApiModelProperty("事项id")
    private Long id;
}
