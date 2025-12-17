package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 根据业务域名称查询业务域组请求
 * @author Qoder
 * @date 2025/12/17
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("根据业务域名称查询业务域组请求")
public class GetBizDomainGroupsByNamesReq extends BaseReq {

    @ApiModelProperty(value = "业务域名称集合", required = true)
    @NotEmpty(message = "业务域名称集合不能为空")
    private List<String> bizDomainNames;
}
