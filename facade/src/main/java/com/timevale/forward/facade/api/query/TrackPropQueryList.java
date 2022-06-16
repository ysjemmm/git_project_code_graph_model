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

    @ApiModelProperty("状态")
    private List<Integer>status;

}
