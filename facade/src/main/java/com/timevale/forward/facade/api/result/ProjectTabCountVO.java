package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
@ApiModel("项目标签页todo数量")
public class ProjectTabCountVO extends ToString {

    @ApiModelProperty("项目风险数量")
    private Long projectRiskCount;

}
