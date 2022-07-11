package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
public class ProjectIdPageQuery extends QueryBase {

    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty("排序字段")
    private String orderFiled;

    @ApiModelProperty("排序规则：0正序，1逆序")
    private Integer orderCollation;

}
