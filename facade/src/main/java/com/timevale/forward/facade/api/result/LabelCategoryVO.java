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
@ApiModel("标签类别信息")
public class LabelCategoryVO extends ToString {

    @ApiModelProperty("id")
    private String id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("模块名称")
    private String typeName;

    @ApiModelProperty("业务域名称")
    private String bizDomainNames;

    @ApiModelProperty("部门名称")
    private String deptNames;

    @ApiModelProperty("打标人员")
    private String markMan;

    @ApiModelProperty("提交人")
    private String createMan;

    @ApiModelProperty("提交人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;
}
