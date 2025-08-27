package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author qiyuan
 * @date 2025-08-25 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务域集查询")
public class BizDomainGroupQueryList extends QueryBase {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("业务域负责人")
    private List<String> ownerIds;

    @ApiModelProperty("上架状态：0-未上架，1-已上架")
    private Integer listingStatus;

}