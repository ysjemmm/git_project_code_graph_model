package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务域查询")
public class BizDomainQueryList extends QueryBase {

    @ApiModelProperty("id（精确搜索）")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("业务域负责人")
    private List<String> ownerIds;

    @ApiModelProperty("上架状态：0-未上架，1-已上架")
    private Integer listingStatus;

}