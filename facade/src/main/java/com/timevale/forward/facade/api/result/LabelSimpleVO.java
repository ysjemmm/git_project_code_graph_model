package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("标签简单信息")
public class LabelSimpleVO extends ToString {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("类别id")
    private Long labelCategoryId;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("提交人")
    private String createMan;

    @ApiModelProperty("提交人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;
}
