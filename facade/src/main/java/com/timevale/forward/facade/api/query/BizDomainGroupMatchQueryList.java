package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author qiyuan
 * @date 2025-08-25 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务域集匹配查询")
public class BizDomainGroupMatchQueryList extends QueryBase {

    @ApiModelProperty(value = "业务域集id")
    private Long bizDomainGroupId;

    @ApiModelProperty("名称")
    private String name;

}