package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("标签信息")
public class LabelVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("标签类别id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long labelCategoryId;

    @ApiModelProperty("标签名称")
    private String name;

    @ApiModelProperty("标签类别名称")
    private String categoryName;

    @ApiModelProperty("模块名称")
    private List<Integer> types;

    @ApiModelProperty("业务域名称")
    private List<String> bizDomains;

    @ApiModelProperty("部门名称")
    private List<String> depts;

    @ApiModelProperty("打标人员")
    private List<String> markMans;

    @ApiModelProperty("提交人")
    private String createMan;

    @ApiModelProperty("提交人id")
    private String createManId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;
}
