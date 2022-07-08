package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("人天新增、修改、删除请求类")
public class ManDayModifyReq extends BaseReq {

    @NotNull(message = "项目id必填")
    @ApiModelProperty("项目id")
    private Long projectId;

    @NotNull(message = "项目成员必填")
    @ApiModelProperty("项目成员花名拼音")
    private String memberId;

    @ApiModelProperty("实际人天")
    private BigDecimal actualManDay;

    @Pattern(regexp = "^\\s*\\d{4}-\\d{2}-\\d{2}\\s*~\\s*\\d{4}-\\d{2}-\\d{2}\\s*$",
            message = "日期范围输入格式不符合规则: yyyy-MM-dd ~ yyyy-MM-dd")
    @NotNull(message = "日期范围必填")
    @ApiModelProperty(value = "日期范围: yyyy-MM-dd ~ yyyy-MM-dd", required = true)
    private String weekDateRange;

}
