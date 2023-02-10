package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;


/**
 * @author by YangXu
 * @date 2023/02/10 16:38
 */
@Getter
@Setter
@ApiModel("项目完成请求")
public class ProjectInnerCompleteReq extends ToString {
    @NotNull(message = "id不能为空")
    @ApiModelProperty("项目id")
    private Long projectId;
}
