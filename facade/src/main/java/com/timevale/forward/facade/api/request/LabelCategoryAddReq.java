package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("标签类别新增")
public class LabelCategoryAddReq extends BaseReq {

    @ApiModelProperty("标签类别名称")
    @NotBlank(message = "模块类型不能为空")
    @Size(max = 100,message = "标签类别长度不能超过100字符")
    private String name;

    @ApiModelProperty("模块类型(10业务需求、11产品需求、12项目、13线下bug、14线上bug)")
    @NotNull(message = "模块类型不能为空")
    private List<Integer> types;

    @ApiModelProperty("业务域")
    @NotNull(message = "业务域不能为空")
    private List<Long> bizDomainIds;

    @ApiModelProperty("打标部门")
    @Size(max = 20,message = "打标部门长度不能超过500字符")
    private List<String> deptIds;

    @ApiModelProperty("打标人员")
    @Size(max = 50,message = "打标人员长度不能超过500字符")
    private List<String>markMans;

    @ApiModelProperty("打标人员花名拼音")
    @Size(max = 50,message = "打标人员长度不能超过500字符")
    private List<String> markManIds;


}
