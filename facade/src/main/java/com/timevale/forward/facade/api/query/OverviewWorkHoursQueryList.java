package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class OverviewWorkHoursQueryList extends QueryBase {

    @ApiModelProperty("项目成员id集合")
    private List<String> memberIds;

    @ApiModelProperty("所属项目Id集合")
    private List<Long> projectIds;

    @ApiModelProperty("投入日期开始时间不能为空")
    @NotNull(message = "所属项目Id集合")
    private Date startDate;

    @ApiModelProperty("投入日期结束时间")
    @NotNull(message = "投入日期结束时间不能为空")
    private Date endDate;

    @ApiModelProperty("是否只显示未登记")
    private Boolean isUnregistered;

}
