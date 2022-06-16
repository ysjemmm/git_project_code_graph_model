package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
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
public class TrackPropVO extends ToString {

    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("属性类型:0新增属性,1已有属性,2默认属性")
    private Integer type;

    @ApiModelProperty("属性类型:-1已撤回,0审核中,1审核通过,2审核不通过")
    private Integer status;

    @ApiModelProperty("状态")
    private String statusName;

    @ApiModelProperty("数据类型")
    private String dataType;
}
