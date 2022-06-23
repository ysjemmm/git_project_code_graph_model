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
@ApiModel("属性列表查询")
public class TrackPropQueryList extends QueryBase {


    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("评审状态:-1已撤回,0审核中,1审核通过,2审核不通过")
    private List<Integer>status;

    @ApiModelProperty("属性类型:0新增属性,1已有属性,2默认属性")
    private List<Integer>types;

}
