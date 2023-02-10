package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/3
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiOperation("团队项目工时看板")
public class HomePageGroupWorkTimeVO extends ToString {

    @ApiModelProperty("用户账户")
    private String executorId;

    @ApiModelProperty("用户花名")
    private String executor;

    @ApiModelProperty("项目任务列表")
    private List<HomePageProjectWorkTimeVO> list;

}
