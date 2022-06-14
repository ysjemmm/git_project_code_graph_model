package com.timevale.forward.facade.api.result;

import com.timevale.forward.facade.api.request.BaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("事件属性信息")
public class TrackPropVO extends BaseReq {

    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("属性类型:0新增属性,1已有属性,2默认属性")
    private Integer type;

    @ApiModelProperty("数据类型")
    private String dataType;
}
