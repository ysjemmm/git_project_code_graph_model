package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2022/03/16 15:21
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("改进措施-完成")
public class ImprovementMeasureCompleteReq extends BaseReq {

    @ApiModelProperty("事项id")
    @NotNull(message = "事项id不能为空")
    private Long id;

    @ApiModelProperty("完成说明")
    @NotBlank(message = "完成说明不能为空")
    private String content;
}
