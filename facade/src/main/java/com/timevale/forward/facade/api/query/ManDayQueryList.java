package com.timevale.forward.facade.api.query;

import com.timevale.forward.facade.api.request.BaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("人天列表查询入参")
public class ManDayQueryList extends BaseReq {

    @Pattern(regexp = "^\\s*\\d{4}-\\d{2}-\\d{2}\\s*~\\s*\\d{4}-\\d{2}-\\d{2}\\s*$",
            message = "日期范围输入格式不符合规则: yyyy-MM-dd ~ yyyy-MM-dd")
    @NotNull(message = "日期范围必填")
    @ApiModelProperty(value = "日期范围: yyyy-MM-dd ~ yyyy-MM-dd", required = true)
    private String weekDateRange;

    @ApiModelProperty("项目id")
    private Long projectId;

}
