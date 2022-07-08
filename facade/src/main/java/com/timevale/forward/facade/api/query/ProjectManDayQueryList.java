package com.timevale.forward.facade.api.query;

import com.timevale.forward.facade.api.request.BaseReq;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author jingchun
 * create on 2022/6/27
 */
@Getter
@Setter
@ApiModel("项目人天列表查询入参")
public class ProjectManDayQueryList extends BaseReq {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id必填")
    private Long projectId;

    @ApiModelProperty("用户id列表")
    private List<String> userIds;

    @ApiModelProperty("日期范围列表")
    private List<String> weekDateRanges;

}
