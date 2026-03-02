package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 产品需求分组归档请求
 * @author kiro
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组归档请求")
public class ProductDemandGroupArchiveReq extends BaseReq {

    @ApiModelProperty(value = "分组ID列表", required = true)
    @NotEmpty(message = "分组ID列表不能为空")
    private List<Long> ids;

    @ApiModelProperty(value = "业务域集ID", required = true)
    @NotNull(message = "业务域集ID不能为空")
    private Long bizDomainGroupId;
}
